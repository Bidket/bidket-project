package com.bidket.order.application.outbox.scheduler;

import com.bidket.order.application.outbox.publisher.OutboxEventPublisher;
import com.bidket.order.domain.outbox.model.OrderOutbox;
import com.bidket.order.domain.outbox.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * OutBox 폴링 스케줄러
 *
 * 주기적으로 발행 가능한 OutBox를 조회하여 Kafka로 발행합니다.
 * - PENDING 상태: 즉시 발행
 * - FAILED 상태: 백오프 전략에 따라 재시도
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingScheduler {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;

    private final OrderOutboxRepository outboxRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final Clock clock;

    /**
     * 주기적으로 발행 대기 중인 OutBox 이벤트를 폴링하여 발행
     */
    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        List<OrderOutbox> candidates = outboxRepository.findReadyToPublish(BATCH_SIZE);
        if (candidates.isEmpty()) {
            return;
        }

        log.debug("[OutboxPollingScheduler] 발행 대기 중인 OutBox {} 건 발견", candidates.size());
        LocalDateTime now = LocalDateTime.now(clock);

        for (OrderOutbox outbox : candidates) {
            if (!outbox.canPublish(MAX_RETRIES, now, clock)) {
                continue;
            }

            try {
                // 1. PUBLISHING 상태로 변경 (낙관적 락)
                OrderOutbox publishing = outbox.markPublishing(now);
                OrderOutbox savedOutbox = outboxRepository.save(publishing);

                // 2. Kafka로 발행
                outboxEventPublisher.publish(savedOutbox);

                // 3. PUBLISHED 상태로 변경
                OrderOutbox published = savedOutbox.markPublished(LocalDateTime.now(clock));
                outboxRepository.save(published);

                log.info("[OutboxPollingScheduler] OutBox 발행 완료: id={}, eventType={}, aggregateId={}",
                        outbox.id(), outbox.eventType(), outbox.aggregateId());

            } catch (RuntimeException e) {
                log.error("[OutboxPollingScheduler] OutBox 발행 실패: id={}, eventType={}, retryCount={}",
                        outbox.id(), outbox.eventType(), outbox.retryCount(), e);

                try {
                    // 실패 시 최신 엔티티를 다시 조회해서 상태 업데이트
                    Optional<OrderOutbox> latestOutboxOpt = outboxRepository.findById(outbox.id());
                    if (latestOutboxOpt.isPresent()) {
                        OrderOutbox latestOutbox = latestOutboxOpt.get();
                        OrderOutbox failed = latestOutbox.markFailed(e.getMessage(), MAX_RETRIES, LocalDateTime.now(clock));
                        outboxRepository.save(failed);

                        if (failed.retryCount() >= MAX_RETRIES) {
                            log.error("[OutboxPollingScheduler] OutBox 최대 재시도 횟수 초과: id={}, eventType={}",
                                    failed.id(), failed.eventType());
                        }
                    } else {
                        log.warn("[OutboxPollingScheduler] OutBox를 찾을 수 없습니다: id={}", outbox.id());
                    }
                } catch (Exception saveException) {
                    log.error("[OutboxPollingScheduler] OutBox 실패 상태 저장 중 예외 발생: id={}", outbox.id(), saveException);
                }
            }
        }
    }
}
