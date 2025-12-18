package com.bidket.queue.application.event;

import com.bidket.queue.application.service.QueueManagementService;
import com.bidket.queue.domain.event.EventTemplate;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionEventConsumer {
    private final ReceiverOptions<String, EventTemplate> baseReceiverOptions;
    private final QueueManagementService managementService;
    private final ObjectMapper objectMapper;

    @Value("${kafka.auction.topic}")
    private String auctionTopic;

    private Disposable disposable;


    @PostConstruct
    public void startConsuming() {
        ReceiverOptions<String, EventTemplate> receiverOptions =baseReceiverOptions
                .subscription(Collections.singleton(auctionTopic));

        disposable = KafkaReceiver.create(receiverOptions)
                .receive()
                .flatMap(this::processAuctionCreate, 20)
                .subscribe(
                        null,
                        e -> log.error("Event Consumer 에러 발생: {}", e.getMessage(), e)
                );
    }

    private Mono<Void> processAuctionCreate(ReceiverRecord<String, EventTemplate> record) {
        return Mono.fromCallable(() ->
                        objectMapper.convertValue(record.value().data(), QueueCreateRequest.class)
                )
                .flatMap(managementService::createConfigQueue)
                .doOnSuccess(isSuccess -> {
                    record.receiverOffset().acknowledge();
                    log.info("경매 생성 이벤트 처리 성공: offset = {}", record.receiverOffset().offset());
                })
                .onErrorResume(e -> {
                    if (e instanceof IllegalArgumentException || e instanceof NullPointerException) {
                        log.error("data -> dto 변환 중 에러 발생: {}", e.getMessage(), e);
                        record.receiverOffset().acknowledge();
                    } else
                        log.error("경매 생성 이벤트 처리 중 에러 발생: offset = {}, message = {}", record.receiverOffset().offset(), e.getMessage(), e);

                    return Mono.empty();
                })
                .then();
    }

    @PreDestroy
    public void close() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
