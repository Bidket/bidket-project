package com.bidket.auction.application.outbox.service;

import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.bidket.auction.domain.outbox.model.OutboxStatus;
import com.bidket.auction.domain.outbox.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxService 단위 테스트")
class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        outboxService = new OutboxService(outboxRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("경매 이벤트를 Outbox에 PENDING 상태로 저장한다")
    void saveAuctionEvent_shouldPersistPendingOutbox() {
        // given
        UUID auctionId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "auctionId", auctionId,
                "status", "CREATING"
        );

        when(outboxRepository.save(any(AuctionOutbox.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        AuctionOutbox saved = outboxService.saveAuctionEvent(
                "AUCTION_CREATED",
                auctionId,
                payload,
                correlationId
        );

        // then
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getAggregateType()).isEqualTo("AUCTION");
        assertThat(saved.getAggregateId()).isEqualTo(auctionId);
        assertThat(saved.getCorrelationId()).isEqualTo(correlationId);
        assertThat(saved.getEventType()).isEqualTo("AUCTION_CREATED");
        assertThat(saved.getPayload()).contains(auctionId.toString());

        verify(outboxRepository).save(any(AuctionOutbox.class));
    }
}
