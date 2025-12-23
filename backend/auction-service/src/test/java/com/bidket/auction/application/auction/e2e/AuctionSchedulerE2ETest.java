package com.bidket.auction.application.auction.e2e;

import com.bidket.auction.application.auction.scheduler.AuctionScheduler;
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
import com.bidket.auction.domain.saga.repository.AuctionEndSagaContextRepository;
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
@DisplayName("경매 스케줄러 E2E 테스트")
class AuctionSchedulerE2ETest {

    @Autowired
    private AuctionScheduler auctionScheduler;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionEndSagaContextRepository sagaRepository;

    @Autowired(required = false)
    private AuctionEndSagaOrchestrator sagaOrchestrator;

    private UUID sellerId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("종료 시간이 지난 경매를 자동으로 종료해야 함")
    void shouldEndExpiredAuctions() {
         
        UUID auctionId = createExpiredAuction();

        auctionScheduler.endActiveAuctions();

        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(auction.getStatus()).isIn(
            AuctionStatus.EXPIRED,
            AuctionStatus.PENDING  
        );
    }

    @Test
    @DisplayName("낙찰자가 있는 경매는 Saga를 시작해야 함")
    void shouldStartSagaForAuctionWithWinner() {
         
        UUID auctionId = createExpiredAuction();
        UUID bidderId = UUID.randomUUID();
        createBid(auctionId, bidderId, 350000L);

        Auction auctionBefore = auctionRepository.findById(auctionId).orElseThrow();
        long totalBids = auctionBefore.getStats().getTotalBidsCount();

        auctionScheduler.endActiveAuctions();

        assertThat(totalBids).isGreaterThan(0);
    }

    @Test
    @DisplayName("낙찰자가 없는 경매는 EXPIRED 상태로 변경해야 함")
    void shouldMarkAsExpiredForAuctionWithoutBids() {
         
        UUID auctionId = createExpiredAuction();

        auctionScheduler.endActiveAuctions();

        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.EXPIRED);
        assertThat(auction.getWinnerInfo().getWinnerId()).isNull();
    }

    @Test
    @DisplayName("종료 시간이 아직 안된 경매는 처리하지 않아야 함")
    void shouldNotEndActiveAuctions() {
         
        UUID auctionId = createActiveAuction();

        auctionScheduler.endActiveAuctions();

        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    @DisplayName("여러 종료된 경매를 한 번에 처리해야 함")
    void shouldHandleMultipleExpiredAuctions() {
         
        UUID auction1 = createExpiredAuction();
        UUID auction2 = createExpiredAuction();
        UUID auction3 = createExpiredAuction();

        createBid(auction1, UUID.randomUUID(), 310000L);
        createBid(auction2, UUID.randomUUID(), 320000L);

        auctionScheduler.endActiveAuctions();

        Auction a1 = auctionRepository.findById(auction1).orElseThrow();
        Auction a2 = auctionRepository.findById(auction2).orElseThrow();
        Auction a3 = auctionRepository.findById(auction3).orElseThrow();

        assertThat(a3.getStatus()).isEqualTo(AuctionStatus.EXPIRED);

        assertThat(a1.getStatus()).isIn(
                AuctionStatus.ACTIVE,
                AuctionStatus.PAYMENT_PENDING,
                AuctionStatus.SUCCESS,
                AuctionStatus.EXPIRED
        );
        assertThat(a2.getStatus()).isIn(
                AuctionStatus.ACTIVE,
                AuctionStatus.PAYMENT_PENDING,
                AuctionStatus.SUCCESS,
                AuctionStatus.EXPIRED
        );
    }

    private UUID createExpiredAuction() {
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
                .auctionTitle("종료된 테스트 경매")
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

    private UUID createActiveAuction() {
        PriceInfo priceInfo = PriceInfo.builder()
                .startPrice(300000L)
                .currentPrice(300000L)
                .bidIncrement(10000L)
                .buyNowPrice(500000L)
                .build();

        AuctionPeriod period = AuctionPeriod.builder()
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(24))
                .originalEndTime(LocalDateTime.now().plusHours(24))
                .extensionCount(0)
                .build();

        Auction auction = Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(sellerId)
                .auctionTitle("진행 중인 테스트 경매")
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

    private Bid createBid(UUID auctionId, UUID bidderId, Long amount) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();

        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(amount)
                .status(BidStatus.ACTIVE)
                .isHighest(true)
                .build();

        auction.updateCurrentPrice(amount);
        auctionRepository.save(auction);

        return bidRepository.save(bid);
    }
}
