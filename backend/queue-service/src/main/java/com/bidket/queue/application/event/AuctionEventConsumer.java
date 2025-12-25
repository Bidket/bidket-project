package com.bidket.queue.application.event;

import com.bidket.queue.application.service.QueueManagementService;
import com.bidket.queue.domain.event.EventTemplate;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionEventConsumer {
    private final ReceiverOptions<String, EventTemplate> baseReceiverOptions;
    private final QueueManagementService managementService;
    private final ObjectMapper objectMapper;
    private final KafkaSender<String, EventTemplate> kafkaSender;

    @Value("${kafka.auction.topic}")
    private String auctionTopic;

    @Value("${kafka.auction.dlt.topic}")
    private String auctionDltTopic;

    private Disposable disposable;


    @PostConstruct
    public void startConsuming() {
        ReceiverOptions<String, EventTemplate> receiverOptions = baseReceiverOptions
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
                        return Mono.empty();
                    } else {
                        log.error("경매 생성 이벤트 처리 중 에러 발생: offset = {}, message = {}", record.receiverOffset().offset(), e.getMessage(), e);
                        return sendToDlt(record, e)
                                .then(Mono.empty());
                    }
                })
                .then();
    }

    private Mono<Void> sendToDlt(ReceiverRecord<String, EventTemplate> record, Throwable e) {
        List<Header> headers = new ArrayList<>();
        if (record.headers() != null) {
            record.headers().forEach(headers::add); // 원본 헤더 유지
        }
        headers.add(new RecordHeader("dlt-original-topic", auctionTopic.getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("dlt-exception-message", e.getMessage().getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("dlt-exception-class", e.getClass().getName().getBytes(StandardCharsets.UTF_8)));

        SenderRecord<String, EventTemplate, Integer> dltRecord = SenderRecord.create(
                new ProducerRecord<>(auctionDltTopic, null, record.key(), record.value(), headers),
                1
        );

        return kafkaSender.send(Mono.just(dltRecord))
                .doOnNext(result -> {
                    if(result.exception() == null) {
                        record.receiverOffset().acknowledge();
                        log.info("DLT 전송 및 처리 성공: offset = {}", record.receiverOffset().offset());
                    } else
                        log.error("DLT 전송 실패", result.exception());
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
