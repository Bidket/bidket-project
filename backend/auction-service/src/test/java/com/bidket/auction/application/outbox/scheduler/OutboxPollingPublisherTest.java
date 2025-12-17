package com.bidket.auction.application.outbox.scheduler;

import com.bidket.auction.application.outbox.publisher.OutboxEventPublisher;
import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.bidket.auction.domain.outbox.model.OutboxStatus;
import com.bidket.auction.domain.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPollingPublisher 단위 테스트")
class OutboxPollingPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    private OutboxPollingPublisher pollingPublisher;

    @BeforeEach
    void setUp() {
        pollingPublisher = new OutboxPollingPublisher(
                outboxRepository,
                outboxEventPublisher,
                Clock.fixed(Instant.parse("2025-12-12T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("PENDING 이벤트를 성공적으로 발행하면 상태가 PUBLISHED로 변경된다")
    void publishPendingEvents_shouldMarkPublishedOnSuccess() {
        // given
        AuctionOutbox outbox = AuctionOutbox.pending(
                "AUCTION",
                UUID.randomUUID(),
                "AUCTION_CREATED",
                "{}",
                UUID.randomUUID()
        );

        when(outboxRepository.findReadyToPublish(50)).thenReturn(List.of(outbox));
        when(outboxRepository.save(any(AuctionOutbox.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        pollingPublisher.publishPendingEvents();

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(outbox.getPublishedAt()).isNotNull();
        verify(outboxEventPublisher).publish(outbox);
        verify(outboxRepository, times(2)).save(any(AuctionOutbox.class));
    }

    @Test
    @DisplayName("발행 실패 시 상태를 FAILED로 변경하고 재시도 횟수를 증가시킨다")
    void publishPendingEvents_shouldIncrementRetryOnFailure() {
        // given
        AuctionOutbox outbox = AuctionOutbox.pending(
                "AUCTION",
                UUID.randomUUID(),
                "AUCTION_CREATED",
                "{}",
                UUID.randomUUID()
        );

        when(outboxRepository.findReadyToPublish(50)).thenReturn(List.of(outbox));
        when(outboxRepository.save(any(AuctionOutbox.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.findById(outbox.getId()))
                .thenReturn(Optional.of(outbox));
        doThrow(new RuntimeException("Kafka unavailable"))
                .when(outboxEventPublisher).publish(any(AuctionOutbox.class));

        // when
        pollingPublisher.publishPendingEvents();

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getPublishedAt()).isNull();
        verify(outboxRepository, times(2)).save(any(AuctionOutbox.class));
        verify(outboxRepository).findById(outbox.getId());
    }
}
