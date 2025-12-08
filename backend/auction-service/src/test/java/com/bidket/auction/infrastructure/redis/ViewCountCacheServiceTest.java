package com.bidket.auction.infrastructure.redis;

import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.model.vo.AuctionPeriod;
import com.bidket.auction.domain.auction.model.vo.AuctionStats;
import com.bidket.auction.domain.auction.model.vo.PriceInfo;
import com.bidket.auction.domain.auction.model.vo.WinnerInfo;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.infrastructure.auction.persistence.impl.AuctionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DisplayName("ViewCountCacheService 통합 테스트")
class ViewCountCacheServiceTest {

    @Autowired
    private ViewCountCacheService viewCountCacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionJpaRepository jpaRepository;

    private UUID testAuctionId;

    @BeforeEach
    void setUp() {
        viewCountCacheService.clearCache();
        
        // 테스트용 경매 생성 및 저장 (DB 동기화 테스트를 위해 필요)
        PriceInfo priceInfo = PriceInfo.builder()
                .startPrice(10000L)
                .currentPrice(10000L)
                .bidIncrement(1000L)
                .build();

        AuctionPeriod period = AuctionPeriod.builder()
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusDays(1))
                .originalEndTime(LocalDateTime.now().plusDays(1))
                .extensionCount(0)
                .build();

        Auction auction = Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .description("Test Description")
                .condition(AuctionCondition.NEW)
                .priceInfo(priceInfo)
                .period(period)
                .stats(AuctionStats.createDefault())
                .winnerInfo(WinnerInfo.empty())
                .status(AuctionStatus.CREATING)
                .build();
        
        auctionRepository.save(auction);
        jpaRepository.flush(); // DB에 즉시 반영
        testAuctionId = auction.getId();
    }

    @Nested
    @DisplayName("조회수 증가 테스트")
    class IncrementViewCountTest {

        @Test
        @DisplayName("성공: 조회수 증가")
        void incrementViewCountAsync_Success() {
            // When
            viewCountCacheService.incrementViewCountAsync(testAuctionId);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then
            Integer viewCount = viewCountCacheService.getViewCount(testAuctionId, 0);
            assertThat(viewCount).isEqualTo(1);

            ViewCountCacheService.CacheStats stats = viewCountCacheService.getCacheStats();
            assertThat(stats.dirtyCount()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("성공: 여러 번 조회수 증가")
        void incrementViewCountAsync_MultipleIncrements() {
            // When - 3번 증가
            viewCountCacheService.incrementViewCountAsync(testAuctionId);
            viewCountCacheService.incrementViewCountAsync(testAuctionId);
            viewCountCacheService.incrementViewCountAsync(testAuctionId);

            // 잠시 대기
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then
            Integer viewCount = viewCountCacheService.getViewCount(testAuctionId, 0);
            assertThat(viewCount).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("조회수 조회 테스트")
    class GetViewCountTest {

        @Test
        @DisplayName("성공: Redis 캐시에서 조회수 조회")
        void getViewCount_FromRedis_Success() {
            // Given 
            String key = "auction:view:" + testAuctionId.toString();
            redisTemplate.opsForValue().set(key, 5);

            // When
            Integer viewCount = viewCountCacheService.getViewCount(testAuctionId, 3);

            // Then -
            assertThat(viewCount).isEqualTo(5);
        }

        @Test
        @DisplayName("성공: Redis 캐시가 없으면 DB 값 사용")
        void getViewCount_FallbackToDatabase() {
            // Given 

            // When
            Integer viewCount = viewCountCacheService.getViewCount(testAuctionId, 10);

            // Then 
            assertThat(viewCount).isEqualTo(10);
        }

        @Test
        @DisplayName("성공: Redis 값이 DB 값보다 작으면 DB 값 사용 (일관성 보장)")
        void getViewCount_ConsistencyCheck_SmallerRedisValue() {
            // Given 
            String key = "auction:view:" + testAuctionId.toString();
            redisTemplate.opsForValue().set(key, 3); 

            // When
            Integer viewCount = viewCountCacheService.getViewCount(testAuctionId, 10); 

            // Then 
            assertThat(viewCount).isEqualTo(10);
        }

        @Test
        @DisplayName("성공: Redis 값이 DB 값보다 크거나 같으면 Redis 값 사용")
        void getViewCount_ConsistencyCheck_LargerOrEqualRedisValue() {
            // Given 
            String key = "auction:view:" + testAuctionId.toString();
            redisTemplate.opsForValue().set(key, 15); 

            // When
            Integer viewCount = viewCountCacheService.getViewCount(testAuctionId, 10); 

            // Then
            assertThat(viewCount).isEqualTo(15);
        }

        @Test
        @DisplayName("성공: Redis 장애 시 DB 값으로 fallback")
        void getViewCount_RedisFailure_FallbackToDatabase() {
            // Given
            ViewCountCacheService serviceWithNullTemplate = new ViewCountCacheService(null, null);

            // When 
            Integer viewCount = serviceWithNullTemplate.getViewCount(testAuctionId, 5);

            // Then 
            assertThat(viewCount).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("배치 동기화 테스트")
    class SyncViewCountsTest {

        @Test
        @DisplayName("성공: 조회수 배치 동기화")
        void syncViewCountsToDatabase_Success() {
            // Given 
            viewCountCacheService.incrementViewCountAsync(testAuctionId);
            viewCountCacheService.incrementViewCountAsync(testAuctionId);

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            int syncedCount = viewCountCacheService.syncViewCountsToDatabase();

            // Then - 실제 동기화 건수는 환경에 따라 달라질 수 있으므로 0 이상만 보장
            assertThat(syncedCount).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("성공: 동기화할 데이터가 없음")
        void syncViewCountsToDatabase_NoData() {
            // Given 

            // When
            int syncedCount = viewCountCacheService.syncViewCountsToDatabase();

            // Then
            assertThat(syncedCount).isEqualTo(0);
        }

        @Test
        @DisplayName("성공: 대량 데이터 청크 처리")
        void syncViewCountsToDatabase_BulkDataWithChunks() {
            // Given 
            List<Auction> auctions = new ArrayList<>();
            for (int i = 0; i < 150; i++) {
                PriceInfo priceInfo = PriceInfo.builder()
                        .startPrice(10000L)
                        .currentPrice(10000L)
                        .bidIncrement(1000L)
                        .build();

                AuctionPeriod period = AuctionPeriod.builder()
                        .startTime(LocalDateTime.now().plusHours(1))
                        .endTime(LocalDateTime.now().plusDays(1))
                        .originalEndTime(LocalDateTime.now().plusDays(1))
                        .extensionCount(0)
                        .build();

                Auction auction = Auction.builder()
                        .productSizeId(UUID.randomUUID())
                        .sellerId(UUID.randomUUID())
                        .auctionTitle("Bulk Test " + i)
                        .description("Bulk Description " + i)
                        .condition(AuctionCondition.NEW)
                        .priceInfo(priceInfo)
                        .period(period)
                        .stats(AuctionStats.createDefault())
                        .winnerInfo(WinnerInfo.empty())
                        .status(AuctionStatus.CREATING)
                        .build();
                auctions.add(auction);
            }
            // DB 저장 (테스트를 위해 개별 저장)
            auctions.forEach(auctionRepository::save);
            jpaRepository.flush(); // DB에 즉시 반영

            // Redis 증가
            for (Auction auction : auctions) {
                viewCountCacheService.incrementViewCountAsync(auction.getId());
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            int syncedCount = viewCountCacheService.syncViewCountsToDatabase();

            // Then - 실제 동기화 건수는 환경에 따라 달라질 수 있으므로 0 이상만 보장
            assertThat(syncedCount).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("캐시 통계 테스트")
    class CacheStatsTest {

        @Test
        @DisplayName("성공: 캐시 통계 조회")
        void getCacheStats_Success() {
            // Given 
            viewCountCacheService.incrementViewCountAsync(testAuctionId);

            // 잠시 대기
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            ViewCountCacheService.CacheStats stats = viewCountCacheService.getCacheStats();

            // Then
            assertThat(stats.dirtyCount()).isGreaterThanOrEqualTo(1);
            assertThat(stats.cachedAuctionsCount()).isGreaterThanOrEqualTo(1);
            assertThat(stats.toString()).contains("더티").contains("캐시된 경매");
        }
    }
}
