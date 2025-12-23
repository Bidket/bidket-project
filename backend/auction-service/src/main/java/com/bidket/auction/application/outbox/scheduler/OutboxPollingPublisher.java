package com.bidket.auction.application.outbox.scheduler;

import com.bidket.auction.application.outbox.publisher.OutboxEventPublisher;
import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.bidket.auction.domain.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingPublisher {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;

    private final OutboxRepository outboxRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        List<AuctionOutbox> candidates = outboxRepository.findReadyToPublish(BATCH_SIZE);
        if (candidates.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        for (AuctionOutbox outbox : candidates) {
            if (!outbox.canPublish(MAX_RETRIES, now, clock)) {
                continue;
            }

            try {
                outbox.markPublishing();
                AuctionOutbox savedOutbox = outboxRepository.save(outbox);

                outboxEventPublisher.publish(savedOutbox);

                savedOutbox.markPublished(LocalDateTime.now(clock));
                outboxRepository.save(savedOutbox);
            } catch (RuntimeException e) {
                log.error("Outbox 발행 실패: id={}, eventType={}, retryCount={}",
                        outbox.getId(), outbox.getEventType(), outbox.getRetryCount(), e);
                try {
                     
                    Optional<AuctionOutbox> latestOutboxOpt = outboxRepository.findById(outbox.getId());
                    if (latestOutboxOpt.isPresent()) {
                        AuctionOutbox latestOutbox = latestOutboxOpt.get();
                        latestOutbox.markFailed(e.getMessage(), MAX_RETRIES);
                        outboxRepository.save(latestOutbox);
                    } else {
                        log.warn("Outbox를 찾을 수 없습니다: id={}", outbox.getId());
                    }
                } catch (Exception saveException) {
                    log.error("Outbox 실패 상태 저장 중 예외 발생: id={}", outbox.getId(), saveException);
                }
            }
        }
    }
}
