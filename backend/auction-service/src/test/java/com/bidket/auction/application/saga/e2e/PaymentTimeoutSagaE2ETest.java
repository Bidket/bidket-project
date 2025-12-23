package com.bidket.auction.application.saga.e2e;

import com.bidket.auction.application.saga.PaymentTimeoutSagaOrchestrator;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.model.vo.AuctionPeriod;
import com.bidket.auction.domain.auction.model.vo.AuctionStats;
import com.bidket.auction.domain.auction.model.vo.PriceInfo;
import com.bidket.auction.domain.auction.model.vo.WinnerInfo;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.model.BidStatus;
import com.bidket.auction.domain.bid.repository.BidRepository;
import com.bidket.auction.domain.outbox.repository.OutboxRepository;
import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaContext;
import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaStep;
import com.bidket.auction.domain.saga.model.SagaStatus;
import com.bidket.auction.domain.saga.repository.PaymentTimeoutSagaContextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("결제 타임아웃 Saga E2E 테스트")
class PaymentTimeoutSagaE2ETest {

    @Autowired
    private PaymentTimeoutSagaOrchestrator sagaOrchestrator;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private PaymentTimeoutSagaContextRepository sagaRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    private UUID sellerId;
    private UUID winnerId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        winnerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("결제 타임아웃 → Saga 시작 → 보상 트랜잭션 실행")
    void shouldStartPaymentTimeoutSagaAndCompensate() {
         
        UUID auctionId = createPaymentWaitingAuction();
        UUID orderId = UUID.randomUUID();

        UUID sagaId = sagaOrchestrator.startPaymentTimeoutSaga(auctionId, orderId);

        PaymentTimeoutSagaContext saga = sagaRepository.findById(sagaId).orElseThrow();
        assertThat(saga).isNotNull();
        assertThat(saga.getAuctionId()).isEqualTo(auctionId);
        assertThat(saga.getOrderId()).isEqualTo(orderId);
        assertThat(saga.getStatus()).isIn(SagaStatus.PENDING, SagaStatus.IN_PROGRESS, SagaStatus.COMPLETED);
    }

    @Test
    @DisplayName("Step 1: REOPEN_AUCTION - 경매 재오픈 (+24시간)")
    void shouldReopenAuctionOnPaymentTimeout() {
         
        UUID auctionId = createPaymentWaitingAuction();
        UUID orderId = UUID.randomUUID();
        Auction auctionBefore = auctionRepository.findById(auctionId).orElseThrow();
        LocalDateTime originalEndTime = auctionBefore.getPeriod().getEndTime();

        UUID sagaId = sagaOrchestrator.startPaymentTimeoutSaga(auctionId, orderId);

        PaymentTimeoutSagaContext saga = sagaRepository.findById(sagaId).orElseThrow();
         
        assertThat(saga.getCurrentStep()).isIn(
                PaymentTimeoutSagaStep.REOPEN_AUCTION,
                PaymentTimeoutSagaStep.REVERT_BID_STATUS,
                PaymentTimeoutSagaStep.CANCEL_ORDER,
                PaymentTimeoutSagaStep.PUBLISH_REOPEN_EVENT
        );
    }

    @Test
    @DisplayName("Step 2: REVERT_BID_STATUS - 낙찰 입찰 정보가 Saga Context에 저장됨")
    void shouldStoreWinningBidInfo() {
         
        UUID auctionId = createPaymentWaitingAuction();
        Bid winningBid = createWinningBid(auctionId, winnerId, 350000L);
        UUID orderId = UUID.randomUUID();

        UUID sagaId = sagaOrchestrator.startPaymentTimeoutSaga(auctionId, orderId);

        PaymentTimeoutSagaContext saga = sagaRepository.findById(sagaId).orElseThrow();
        assertThat(saga).isNotNull();
        assertThat(saga.getAuctionId()).isEqualTo(auctionId);
    }

    @Test
    @DisplayName("Saga Context에 올바른 정보가 저장되어야 함")
    void shouldStoreCorrectSagaContext() {
         
        UUID auctionId = createPaymentWaitingAuction();
        UUID orderId = UUID.randomUUID();

        UUID sagaId = sagaOrchestrator.startPaymentTimeoutSaga(auctionId, orderId);

        PaymentTimeoutSagaContext saga = sagaRepository.findById(sagaId).orElseThrow();
        assertThat(saga.getAuctionId()).isEqualTo(auctionId);
        assertThat(saga.getOrderId()).isEqualTo(orderId);
         
        assertThat(saga.getCurrentStep()).isIn(
                PaymentTimeoutSagaStep.REOPEN_AUCTION,
                PaymentTimeoutSagaStep.REVERT_BID_STATUS,
                PaymentTimeoutSagaStep.CANCEL_ORDER,
                PaymentTimeoutSagaStep.PUBLISH_REOPEN_EVENT
        );
        assertThat(saga.getRetryCount()).isEqualTo(0);
    }

    private UUID createPaymentWaitingAuction() {
        PriceInfo priceInfo = PriceInfo.builder()
                .startPrice(300000L)
                .currentPrice(350000L)
                .bidIncrement(10000L)
                .buyNowPrice(500000L)
                .build();

        AuctionPeriod period = AuctionPeriod.builder()
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().minusHours(2))
                .originalEndTime(LocalDateTime.now().minusDays(1).plusDays(7))
                .extensionCount(0)
                .build();

        Auction auction = Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(sellerId)
                .auctionTitle("결제 대기 테스트 경매")
                .condition(AuctionCondition.NEW)
                .priceInfo(priceInfo)
                .period(period)
                .stats(AuctionStats.createDefault())
                .winnerInfo(WinnerInfo.empty())
                .status(AuctionStatus.CREATING)
                .build();

        auction.confirmCreation();
        auction.start();

        Auction savedAuction = auctionRepository.save(auction);
        UUID auctionId = savedAuction.getId();

        Bid winningBid = createWinningBid(auctionId, winnerId, 350000L);

        savedAuction = auctionRepository.findById(auctionId).orElseThrow();
        savedAuction.end(true);  
        savedAuction.setWinner(winnerId, winningBid.getId(), 350000L);  
        auctionRepository.save(savedAuction);

        return auctionId;
    }

    private Bid createWinningBid(UUID auctionId, UUID bidderId, Long amount) {
        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(amount)
                .status(BidStatus.WON)
                .isHighest(true)
                .build();

        return bidRepository.save(bid);
    }
}
