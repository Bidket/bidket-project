package com.bidket.auction.application.outbox.e2e;

import com.bidket.auction.application.outbox.scheduler.OutboxPollingPublisher;
import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.bidket.auction.domain.outbox.model.OutboxStatus;
import com.bidket.auction.domain.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Outbox 폴링 및 재시도 E2E 테스트")
class OutboxPollingE2ETest {

    @Autowired
    private OutboxPollingPublisher outboxPublisher;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired(required = false)
    private Clock clock;

    @Test
    @DisplayName("PENDING 상태의 Outbox 이벤트를 조회할 수 있어야 함")
    void shouldFindPendingOutboxEvents() {
         
        AuctionOutbox outbox = createPendingOutbox();

        var readyEvents = outboxRepository.findReadyToPublish(10);

        assertThat(readyEvents).isNotEmpty();
    }

    @Test
    @DisplayName("OutboxPollingPublisher가 정상 작동해야 함")
    void shouldWorkOutboxPollingPublisher() {
         
        AuctionOutbox outbox = createPendingOutbox();

        assertThat(outboxPublisher).isNotNull();

        outboxPublisher.publishPendingEvents();
    }

    @Test
    @DisplayName("Outbox canPublish 메서드가 올바르게 동작해야 함")
    void shouldWorkCanPublishMethod() {
         
        AuctionOutbox outbox = createPendingOutbox();

        Clock testClock = clock != null ? clock : Clock.systemUTC();
        boolean canPublish = outbox.canPublish(3, LocalDateTime.now(testClock), testClock);

        assertThat(canPublish).isTrue();
    }

    @Test
    @DisplayName("재시도 횟수가 maxRetries를 초과하면 발행할 수 없어야 함")
    void shouldNotPublishWhenExceedsMaxRetries() {
         
        AuctionOutbox outbox = createPendingOutbox();

        outbox.markPublishing();
        outbox.markFailed("Test error 1", 3);
        outbox.markPublishing();
        outbox.markFailed("Test error 2", 3);
        outbox.markPublishing();
        outbox.markFailed("Test error 3", 3);

        outboxRepository.save(outbox);

        Clock testClock = clock != null ? clock : Clock.systemUTC();
        boolean canPublish = outbox.canPublish(3, LocalDateTime.now(testClock), testClock);

        assertThat(canPublish).isFalse();
    }

    @Test
    @DisplayName("Outbox 상태 전이가 올바르게 동작해야 함")
    void shouldTransitOutboxStatusCorrectly() {
         
        AuctionOutbox outbox = createPendingOutbox();

        outbox.markPublishing();

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHING);

        outbox.markPublished(LocalDateTime.now());

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(outbox.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Outbox 실패 시 재시도 카운트가 증가해야 함")
    void shouldIncrementRetryCountOnFailure() {
         
        AuctionOutbox outbox = createPendingOutbox();
        outbox.markPublishing();
        int initialRetryCount = outbox.getRetryCount();

        outbox.markFailed("Test error", 3);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(initialRetryCount + 1);
    }

    private AuctionOutbox createPendingOutbox() {
        AuctionOutbox outbox = AuctionOutbox.pending(
                "Auction",
                UUID.randomUUID(),
                "TEST_EVENT",
                "{\"test\": \"data\"}",
                UUID.randomUUID()
        );

        return outboxRepository.save(outbox);
    }
}
