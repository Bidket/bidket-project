package com.bidket.auction.application.saga.e2e;

import com.bidket.auction.application.saga.AuctionEndSagaOrchestrator;
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
import com.bidket.auction.domain.saga.model.AuctionEndSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;
import com.bidket.auction.domain.saga.model.SagaStep;
import com.bidket.auction.domain.saga.repository.AuctionEndSagaContextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("경매 종료 Saga E2E 테스트")
class AuctionEndSagaE2ETest {

    @Autowired
    private AuctionEndSagaOrchestrator sagaOrchestrator;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionEndSagaContextRepository sagaRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    private UUID sellerId;
    private UUID bidderId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        bidderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("경매 종료 → 주문 생성 → 낙찰 확정 전체 흐름")
    void shouldCompleteAuctionEndSagaSuccessfully() {
         
        UUID auctionId = createActiveAuction();
        Bid bid = placeBid(auctionId, bidderId, 350000L);

        UUID sagaId = sagaOrchestrator.startAuctionEndSaga(auctionId);

        AuctionEndSagaContext saga = sagaRepository.findById(sagaId).orElseThrow();
        assertThat(saga).isNotNull();
        assertThat(saga.getAuctionId()).isEqualTo(auctionId);
        assertThat(saga.getWinnerId()).isEqualTo(bidderId);
        assertThat(saga.getWinningBidId()).isEqualTo(bid.getId());
        assertThat(saga.getFinalPrice()).isEqualTo(350000L);

        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.CREATE_ORDER);
        assertThat(saga.getStatus()).isIn(SagaStatus.PENDING, SagaStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("낙찰자 없는 경매는 Saga가 시작되지 않아야 함")
    void shouldNotStartSagaForAuctionWithoutBids() {
         
        UUID auctionId = createActiveAuction();

        try {
            UUID sagaId = sagaOrchestrator.startAuctionEndSaga(auctionId);
             
            assertThat(sagaId).isNull();
        } catch (Exception e) {
             
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Step 1: CREATE_ORDER - Saga가 CREATE_ORDER 단계로 시작")
    void shouldStartWithCreateOrderStep() {
         
        UUID auctionId = createActiveAuction();
        placeBid(auctionId, bidderId, 350000L);

        UUID sagaId = sagaOrchestrator.startAuctionEndSaga(auctionId);

        AuctionEndSagaContext saga = sagaRepository.findById(sagaId).orElseThrow();
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.CREATE_ORDER);
    }

    @Test
    @DisplayName("Saga 초기 상태 검증")
    void shouldHaveCorrectInitialState() {
         
        UUID auctionId = createActiveAuction();
        Bid bid = placeBid(auctionId, bidderId, 350000L);

        UUID sagaId = sagaOrchestrator.startAuctionEndSaga(auctionId);

        AuctionEndSagaContext saga = sagaRepository.findById(sagaId).orElseThrow();
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.CREATE_ORDER);
        assertThat(saga.getRetryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("보상 트랜잭션: 주문 생성 실패 시 경매 재오픈")
    void shouldCompensateOnOrderCreationFailure() {
         
        UUID auctionId = createActiveAuction();
        placeBid(auctionId, bidderId, 350000L);
        UUID sagaId = sagaOrchestrator.startAuctionEndSaga(auctionId);

        sagaOrchestrator.compensate(sagaId, "주문 생성 실패");

        AuctionEndSagaContext saga = sagaRepository.findById(sagaId).orElseThrow();
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);

        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
         
        assertThat(auction.getStatus()).isIn(AuctionStatus.ACTIVE, AuctionStatus.PENDING);
    }

    @Test
    @DisplayName("여러 입찰 중 최고가 입찰자가 낙찰자가 되어야 함")
    void shouldSelectHighestBidderAsWinner() {
         
        UUID auctionId = createActiveAuction();
        placeBid(auctionId, UUID.randomUUID(), 310000L);
        placeBid(auctionId, UUID.randomUUID(), 320000L);
        Bid highestBid = placeBid(auctionId, bidderId, 350000L);
        placeBid(auctionId, UUID.randomUUID(), 330000L);  

        UUID sagaId = sagaOrchestrator.startAuctionEndSaga(auctionId);

        AuctionEndSagaContext saga = sagaRepository.findById(sagaId).orElseThrow();
        assertThat(saga.getWinnerId()).isEqualTo(bidderId);
        assertThat(saga.getWinningBidId()).isEqualTo(highestBid.getId());
        assertThat(saga.getFinalPrice()).isEqualTo(350000L);
    }

    private UUID createActiveAuction() {
        PriceInfo priceInfo = PriceInfo.builder()
                .startPrice(300000L)
                .currentPrice(300000L)
                .bidIncrement(10000L)
                .buyNowPrice(500000L)
                .build();

        AuctionPeriod period = AuctionPeriod.builder()
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().minusHours(1))  
                .originalEndTime(LocalDateTime.now().minusDays(1).plusDays(7))
                .extensionCount(0)
                .build();

        Auction auction = Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(sellerId)
                .auctionTitle("E2E 테스트 경매")
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
        return savedAuction.getId();
    }

    private Bid placeBid(UUID auctionId, UUID bidderId, Long amount) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();

        boolean isHigherBid = amount > auction.getPriceInfo().getCurrentPrice();

        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(amount)
                .status(isHigherBid ? BidStatus.ACTIVE : BidStatus.OUTBID)
                .isHighest(isHigherBid)
                .build();

        if (isHigherBid) {
             
            List<Bid> existingBids = bidRepository.findByAuctionId(auctionId);
            existingBids.forEach(existingBid -> {
                if (existingBid.isHighest()) {
                    existingBid.markAsOutbid();
                    bidRepository.save(existingBid);
                }
            });

            auction.updateCurrentPrice(amount);
            auctionRepository.save(auction);
        }

        return bidRepository.save(bid);
    }
}
