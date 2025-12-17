package com.bidket.auction.application.bid.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DisplayName("BID-006: 입찰 동시성 제어 테스트")
class BidConcurrencyTest {

    @Autowired
    private BidService bidService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    private UUID auctionId;
    private UUID sellerId;

    @BeforeEach
    @Transactional
    void setUp() {
        sellerId = UUID.randomUUID();

        PriceInfo priceInfo = PriceInfo.builder()
                .startPrice(300000L)
                .currentPrice(300000L)
                .bidIncrement(10000L)
                .build();

        AuctionPeriod period = AuctionPeriod.builder()
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .originalEndTime(LocalDateTime.now().plusHours(1))
                .extensionCount(0)
                .build();

        Auction auction = Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(sellerId)
                .auctionTitle("동시성 테스트 경매")
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
        auctionId = savedAuction.getId();
    }

    @Test
    @DisplayName("동시에 여러 입찰이 발생해도 정확히 하나의 최고가만 존재한다")
    void shouldHandleConcurrentBids() throws Exception {
        // Given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // When
        for (int i = 0; i < threadCount; i++) {
            final int bidderIndex = i;
            final Long bidAmount = 310000L + (bidderIndex * 10000L);
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    UUID bidderId = UUID.randomUUID();
                    bidService.placeBid(auctionId, bidderId, bidAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }, executorService);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        // Then
        List<Bid> allBids = bidRepository.findByAuctionId(auctionId);
        long highestBidCount = allBids.stream()
                .filter(Bid::isHighest)
                .count();

        assertThat(highestBidCount).isEqualTo(1);
        assertThat(successCount.get()).isGreaterThan(0);
        
        Bid highestBid = allBids.stream()
                .filter(Bid::isHighest)
                .findFirst()
                .orElseThrow();
        
        assertThat(highestBid.getStatus()).isEqualTo(BidStatus.ACTIVE);
    }

    @Test
    @DisplayName("동일한 금액으로 동시 입찰 시 하나만 성공한다")
    void shouldHandleConcurrentBidsWithSameAmount() throws Exception {
        // Given
        int threadCount = 5;
        Long sameAmount = 350000L;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // When
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    UUID bidderId = UUID.randomUUID();
                    bidService.placeBid(auctionId, bidderId, sameAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 동시성 실패는 예상된 동작
                }
            }, executorService);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        // Then
        List<Bid> allBids = bidRepository.findByAuctionId(auctionId);
        long highestBidCount = allBids.stream()
                .filter(Bid::isHighest)
                .count();

        assertThat(highestBidCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Optimistic Lock으로 동시성을 제어한다")
    void shouldUseOptimisticLock() {
        // Given
        UUID bidderId1 = UUID.randomUUID();
        UUID bidderId2 = UUID.randomUUID();

        Bid firstBid = bidService.placeBid(auctionId, bidderId1, 310000L);

        // When & Then
        // Version을 확인하여 Optimistic Lock이 적용되어 있는지 검증
        assertThat(firstBid.getVersion()).isNotNull();

        // 두 번째 입찰이 첫 번째를 밀어낼 때
        bidService.placeBid(auctionId, bidderId2, 320000L);

        // 첫 번째 입찰의 상태가 OUTBID로 변경되었는지 확인
        Bid updatedFirstBid = bidRepository.findById(firstBid.getId()).orElseThrow();
        assertThat(updatedFirstBid.getStatus()).isEqualTo(BidStatus.OUTBID);
        assertThat(updatedFirstBid.getVersion()).isGreaterThan(firstBid.getVersion());
    }
}

