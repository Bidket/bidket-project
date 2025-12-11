package com.bidket.auction.application.auction.scheduler;

import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.model.vo.AuctionPeriod;
import com.bidket.auction.domain.auction.model.vo.AuctionStats;
import com.bidket.auction.domain.auction.model.vo.PriceInfo;
import com.bidket.auction.domain.auction.model.vo.WinnerInfo;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.infrastructure.redis.ViewCountCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionScheduler 테스트 - AUC-011 동일 productSizeId 순차 운영")
class AuctionSchedulerTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private ViewCountCacheService viewCountCacheService;

    @InjectMocks
    private AuctionScheduler auctionScheduler;

    private UUID productSizeId;
    private UUID anotherProductSizeId;

    @BeforeEach
    void setUp() {
        productSizeId = UUID.randomUUID();
        anotherProductSizeId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("동일 productSizeId에 대해 하나의 경매만 ACTIVE로 시작")
    class SingleActivePerProductSize {

        @Test
        @DisplayName("동일 productSizeId에 여러 PENDING 경매가 있을 때 하나만 시작한다")
        void startOnlyOnePendingAuctionPerProductSize() {
            // Given
            Auction pending1 = createPendingAuction(productSizeId);
            Auction pending2 = createPendingAuction(productSizeId);

            given(auctionRepository.findPendingAuctionsStartingBefore(any(LocalDateTime.class)))
                    .willReturn(List.of(pending1, pending2));
            given(auctionRepository.findByStatus(AuctionStatus.ACTIVE))
                    .willReturn(List.of());

            // When
            auctionScheduler.startPendingAuctions();

            // Then
            int activeCount = 0;
            if (pending1.getStatus() == AuctionStatus.ACTIVE) activeCount++;
            if (pending2.getStatus() == AuctionStatus.ACTIVE) activeCount++;

            assertThat(activeCount).isEqualTo(1);
            verify(auctionRepository).findPendingAuctionsStartingBefore(any(LocalDateTime.class));
            verify(auctionRepository).findByStatus(AuctionStatus.ACTIVE);
        }

        @Test
        @DisplayName("이미 ACTIVE 경매가 있는 productSizeId에 대해서는 새로운 경매를 시작하지 않는다")
        void doNotStartPendingWhenActiveExistsForSameProductSize() {
            // Given
            Auction active = createActiveAuction(productSizeId);
            Auction pendingSameSize = createPendingAuction(productSizeId);
            Auction pendingOtherSize = createPendingAuction(anotherProductSizeId);

            given(auctionRepository.findPendingAuctionsStartingBefore(any(LocalDateTime.class)))
                    .willReturn(List.of(pendingSameSize, pendingOtherSize));
            given(auctionRepository.findByStatus(AuctionStatus.ACTIVE))
                    .willReturn(List.of(active));

            // When
            auctionScheduler.startPendingAuctions();

            // Then
            // same size: 여전히 PENDING
            assertThat(pendingSameSize.getStatus()).isEqualTo(AuctionStatus.PENDING);
            // 다른 size: ACTIVE 로 전환
            assertThat(pendingOtherSize.getStatus()).isEqualTo(AuctionStatus.ACTIVE);

            verify(auctionRepository).findPendingAuctionsStartingBefore(any(LocalDateTime.class));
            verify(auctionRepository).findByStatus(AuctionStatus.ACTIVE);
            verify(auctionRepository, never()).save(pendingSameSize);
            verify(auctionRepository).save(pendingOtherSize);
        }
    }

    private Auction createPendingAuction(UUID productSizeId) {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(10);
        LocalDateTime endTime = startTime.plusHours(1);

        PriceInfo priceInfo = PriceInfo.builder()
                .startPrice(100_000L)
                .currentPrice(100_000L)
                .bidIncrement(10_000L)
                .build();

        AuctionPeriod period = AuctionPeriod.builder()
                .startTime(startTime)
                .endTime(endTime)
                .originalEndTime(endTime)
                .extensionCount(0)
                .build();

        Auction auction = Auction.builder()
                .productSizeId(productSizeId)
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .description("desc")
                .condition(AuctionCondition.NEW)
                .priceInfo(priceInfo)
                .period(period)
                .stats(AuctionStats.createDefault())
                .winnerInfo(WinnerInfo.empty())
                .status(AuctionStatus.PENDING)
                .build();

        return auction;
    }

    private Auction createActiveAuction(UUID productSizeId) {
        Auction auction = createPendingAuction(productSizeId);
        auction.start();
        return auction;
    }
}


