package com.bidket.queue.infrastructure.schedule;

import com.bidket.queue.domain.event.EventTemplate;
import com.bidket.queue.domain.event.QueueEnteredNotificationEvent;
import com.bidket.queue.domain.repository.QueueManagementRepository;
import com.bidket.queue.domain.repository.QueueTrafficRepository;
import com.bidket.queue.global.util.jwt.TokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueScheduler {
    private final QueueManagementRepository managementRepository;
    private final QueueTrafficRepository trafficRepository;
    private final TokenProvider tokenProvider;
    private final ReactiveKafkaProducerTemplate<String, EventTemplate> kafkaTemplate;
    private final Sinks.Many<EventTemplate> eventSink = Sinks.many().multicast().onBackpressureBuffer();
    private final ObjectMapper objectMapper;

    private final String EVENT_SOURCE = "queue-service";

    @Value("${kafka.notification.queue.admitted.topic}")
    private String queueEnterTopic;

    @PostConstruct
    public void init() {
        eventSink.asFlux()
                .flatMap(event -> kafkaTemplate.send(queueEnterTopic, event.userId().toString(), event))
                .doOnComplete(() -> log.info("complete"))
                .doOnError(e -> log.error("알림 이벤트 발행 실패: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }

    @Scheduled(fixedDelay = 1000)
    public void entranceSchedule() {
        log.info("스케줄링 시작");
        managementRepository.getAllActiveAuctions()
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(auctionId ->
                        clearActiveQueue(auctionId)
                                .doOnError(e -> log.error("경매 [{}] Active Queue 정리 실패", auctionId, e))
                                .onErrorResume(e -> Mono.empty())
                                .then(enterProcess(auctionId))
                )
                .subscribe(
                        null,
                        e -> log.error("대기열 입장 스케줄러 에러", e)
                );
    }

    private Mono<Void> enterProcess(UUID auctionId) {
        String activeKey = "queue:auction:" + auctionId + ":active";
        String waitingKey = "queue:auction:" + auctionId + ":waiting";

        return managementRepository.getConfig(auctionId)
                .flatMap(config ->
                        trafficRepository.getActiveUserCount(auctionId)
                                .flatMap(currentActive -> {
                                    long maxUser = config.maxActive();
                                    long availableSlots = maxUser - currentActive;
                                    long limit = Math.min(availableSlots, config.permitsPerSec());

                                    if (limit <= 0)
                                        return Mono.just(0L);

                                    return trafficRepository.popUserIdWaitingQueue(waitingKey, limit)
                                            .flatMap(userIds -> {
                                                if (userIds.isEmpty())
                                                    return Mono.just(0L);

                                                Map<UUID, String> userTokens = new HashMap<>();
                                                userIds.forEach(userId -> {
                                                    String token = tokenProvider.generateToken(userId, auctionId);
                                                    userTokens.put(userId, token);
                                                });

                                                log.info("경매[{}] {} 명 입장", auctionId, userIds.size());
                                                return trafficRepository.addAllActiveUser(activeKey, userIds)
                                                        .then(trafficRepository.saveToken(auctionId, userTokens))
                                                        // TODO 입장 시 Active Queue TTL 연장 정책 확립해야함
                                                        .then(managementRepository.setExpiration(activeKey, Instant.now().plus(1, ChronoUnit.HOURS)))
                                                        .doOnSuccess(isSuccess -> {
                                                            userIds.forEach(userId -> {
                                                                QueueEnteredNotificationEvent eventData = QueueEnteredNotificationEvent.builder()
                                                                        .auctionId(auctionId)
                                                                        .userId(userId)
                                                                        .enterTime(LocalDateTime.now())
                                                                        .build();

                                                                EventTemplate event = EventTemplate.builder()
                                                                        .eventId(UUID.randomUUID())
                                                                        .occurredAt(LocalDateTime.now())
                                                                        .source(EVENT_SOURCE)
                                                                        .userId(userId)
                                                                        .data(objectMapper.convertValue(eventData, Map.class))
                                                                        .build();

                                                                eventSink.emitNext(event, Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(100L)));
                                                            });
                                                            log.info("경매[{}] 알림 이벤트 발행 완료: {}명", auctionId, userIds.size());
                                                        });
                                            });

                                })

                )
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("경매[{}] 설정 만료, 관리 목록에서 제거", auctionId);

                    return managementRepository.removeActiveAuction(auctionId)
                            .then(Mono.empty());
                }))
                .then();
    }

    private Mono<Void> clearActiveQueue(UUID auctionId) {
        return trafficRepository.getExpiredActiveUser(auctionId)
                .collectList()
                .flatMap(expiredUserIds -> {
                    if (expiredUserIds.isEmpty())
                        return Mono.empty();

                    return trafficRepository.removeActiveUsers(auctionId, expiredUserIds);
                })
                .then();
    }
}
