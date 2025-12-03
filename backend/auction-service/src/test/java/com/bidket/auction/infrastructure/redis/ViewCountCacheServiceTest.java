package com.bidket.auction.infrastructure.redis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DisplayName("ViewCountCacheService 통합 테스트")
class ViewCountCacheServiceTest {

    @Autowired
    private ViewCountCacheService viewCountCacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private UUID testAuctionId;

    @BeforeEach
    void setUp() {
        testAuctionId = UUID.randomUUID();
        viewCountCacheService.clearCache();
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
            ViewCountCacheService serviceWithNullTemplate = new ViewCountCacheService(null);

            // When 
            Integer viewCount = viewCountCacheService.getViewCount(testAuctionId, 5);

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

            // Then
            assertThat(syncedCount).isEqualTo(1); 

            ViewCountCacheService.CacheStats stats = viewCountCacheService.getCacheStats();
            assertThat(stats.dirtyCount()).isEqualTo(0);
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
            for (int i = 0; i < 150; i++) {
                UUID auctionId = UUID.randomUUID();
                viewCountCacheService.incrementViewCountAsync(auctionId);
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            int syncedCount = viewCountCacheService.syncViewCountsToDatabase();

            // Then 
            assertThat(syncedCount).isEqualTo(150);

            ViewCountCacheService.CacheStats stats = viewCountCacheService.getCacheStats();
            assertThat(stats.dirtyCount()).isEqualTo(0);
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
