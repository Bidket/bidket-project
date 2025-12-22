package com.bidket.queue.infrastructure.schedule;

import com.bidket.queue.domain.event.EventTemplate;
import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.domain.model.outbox.OutboxStatus;
import com.bidket.queue.domain.model.outbox.QueueOutboxModel;
import com.bidket.queue.domain.repository.QueueOutboxRepository;
import com.bidket.queue.infrastructure.persistence.entity.QueueOutboxEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private final QueueOutboxRepository outboxRepository;
    private final KafkaSender<String, EventTemplate> kafkaSender;
    private final ObjectMapper objectMapper;

    @Value("${kafka.notification.queue.admitted.topic}")
    private String queueEnterTopic;

    @Scheduled(fixedDelay = 1000)
    public void publishEvent() {
        outboxRepository.findAllByStatusAndRetryCountLessThan(OutboxStatus.PENDING, 5)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(entity -> {
                    QueueOutboxModel outboxModel = entity.toModel();
                    return publishEvent(outboxModel)
                            .flatMap(this::updatePublishedOutbox)
                            .then()
                            .onErrorResume(e -> {
                                log.error("이벤트 발행 실패: 재시도 실행", e);
                                return retry(entity.toModel())
                                        .onErrorMap(retryException -> {
                                            log.error("Outbox 재시도 횟수 최대치 도달: {}", outboxModel.getId(), retryException);
                                            return new QueueException(QueueErrorCode.OUTBOX_MAX_RETRY);
                                        })
                                        .then();
                            });
                })
                .subscribe(
                        null,
                        e -> log.error("이벤트 발행 실패", e)
                );

    }

    protected Mono<QueueOutboxModel> publishEvent(QueueOutboxModel outboxModel) {
        EventTemplate event = objectMapper.convertValue(outboxModel.getPayload(), EventTemplate.class);

        SenderRecord<String, EventTemplate, UUID> record = SenderRecord.create(
                new ProducerRecord<>(queueEnterTopic, event.eventId().toString(), event),
                event.eventId()
        );

        return kafkaSender.send(Mono.just(record))
                .doOnNext(r -> log.debug("이벤트 발행 성공: correctionId = {}", r.correlationMetadata()))
                .next()
                .thenReturn(outboxModel);
    }

    @Transactional
    protected Mono<QueueOutboxModel> updatePublishedOutbox(QueueOutboxModel model) {
        model.published();
        return outboxRepository.save(QueueOutboxEntity.from(model))
                .map(QueueOutboxEntity::toModel);
    }

    @Transactional
    protected Mono<Integer> retry(QueueOutboxModel model) {
        model.retry();
        if(model.getStatus().equals(OutboxStatus.FAILED))
            return Mono.error(new QueueException(QueueErrorCode.OUTBOX_MAX_RETRY));
        QueueOutboxEntity entity = QueueOutboxEntity.from(model);
        return outboxRepository.save(entity)
                .map(queueOutboxEntity -> queueOutboxEntity.toModel().getRetryCount());
    }
}
