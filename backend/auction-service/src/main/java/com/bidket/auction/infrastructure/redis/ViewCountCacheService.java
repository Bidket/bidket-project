package com.bidket.auction.infrastructure.redis;

import com.bidket.auction.domain.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AuctionRepository auctionRepository;

    private static final String VIEW_COUNT_KEY_PREFIX = "auction:view:";
    private static final String DIRTY_FLAG_KEY = "auction:view:dirty";
    private static final String BATCH_KEY_PREFIX = "auction:view:batch:";

    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final int BATCH_SIZE = 100;

    @Async
    public void incrementViewCountAsync(UUID auctionId) {
        try {
            String key = VIEW_COUNT_KEY_PREFIX + auctionId.toString();

            Long newViewCount = redisTemplate.opsForValue().increment(key);
            if (newViewCount == null) {
                log.warn("조회수 증가 실패: Redis increment 반환값 null, auctionId={}", auctionId);
                return;
            }

            log.debug("조회수 증가: auctionId={}, newCount={}", auctionId, newViewCount);

            if (newViewCount == 1) {
                try {
                    redisTemplate.expire(key, CACHE_TTL);
                } catch (Exception e) {
                    log.warn("TTL 설정 실패: auctionId={}, error={}", auctionId, e.getMessage());
                }
            }

            try {
                redisTemplate.opsForSet().add(DIRTY_FLAG_KEY, auctionId.toString());
            } catch (Exception e) {
                log.warn("더티 플래그 설정 실패: auctionId={}, error={}", auctionId, e.getMessage());
            }

        } catch (Exception e) {
            log.error("Redis 조회수 증가 실패 - 시스템은 계속 동작: auctionId={}, error={}",
                     auctionId, e.getMessage());
        }
    }

    public Integer getViewCount(UUID auctionId, Integer dbViewCount) {
        try {
            String key = VIEW_COUNT_KEY_PREFIX + auctionId.toString();

            Object cachedCount = redisTemplate.opsForValue().get(key);

            if (cachedCount != null) {
                Integer redisCount = Integer.valueOf(cachedCount.toString());

                if (redisCount >= dbViewCount) {
                    log.debug("Redis 조회수 사용: auctionId={}, redis={}, db={}",
                             auctionId, redisCount, dbViewCount);
                    return redisCount;
                } else {
                    log.warn("조회수 일관성 위반 감지: auctionId={}, redis={}, db={} (DB 값 사용)",
                            auctionId, redisCount, dbViewCount);
                    return dbViewCount;
                }
            } else {
                log.debug("Redis 캐시 없음, DB 값 사용: auctionId={}, db={}",
                         auctionId, dbViewCount);
            }
        } catch (Exception e) {
            log.warn("Redis 조회수 조회 실패, DB 값으로 fallback: auctionId={}, db={}, error={}",
                    auctionId, dbViewCount, e.getMessage());
        }

        return dbViewCount;
    }

    public int syncViewCountsToDatabase() {
        try {
            Set<Object> dirtyAuctionIds = redisTemplate.opsForSet().members(DIRTY_FLAG_KEY);

            if (dirtyAuctionIds == null || dirtyAuctionIds.isEmpty()) {
                return 0;
            }

            log.info("조회수 배치 동기화 시작: 총 {} 건", dirtyAuctionIds.size());

            List<Object> auctionIdList = new ArrayList<>(dirtyAuctionIds);

            String batchDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String batchKey = BATCH_KEY_PREFIX + batchDate;

            int totalSyncedCount = 0;

            for (int i = 0; i < auctionIdList.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, auctionIdList.size());
                List<Object> chunk = auctionIdList.subList(i, endIndex);

                int chunkSyncedCount = processBatchChunk(chunk, batchKey);

                totalSyncedCount += chunkSyncedCount;

                if (i + BATCH_SIZE < auctionIdList.size()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                log.debug("청크 동기화 완료: {}/{} 건 (총: {})",
                         chunkSyncedCount, chunk.size(), totalSyncedCount);
            }

            redisTemplate.expire(batchKey, Duration.ofDays(7));

            log.info("조회수 배치 동기화 완료: 총 {} 건", totalSyncedCount);
            return totalSyncedCount;

        } catch (Exception e) {
            log.error("조회수 배치 동기화 중 오류 발생", e);
            return 0;
        }
    }

    @Transactional
    private int processBatchChunk(List<Object> chunkAuctionIds, String batchKey) {
        int syncedCount = 0;

        for (Object auctionIdObj : chunkAuctionIds) {
            String auctionIdStr = auctionIdObj.toString();
            String viewKey = VIEW_COUNT_KEY_PREFIX + auctionIdStr;

            try {
                Object viewCountObj = redisTemplate.opsForValue().get(viewKey);
                if (viewCountObj != null) {
                    Integer viewCount = Integer.valueOf(viewCountObj.toString());
                    UUID auctionId = UUID.fromString(auctionIdStr);

                    int updatedCount = auctionRepository.updateViewCount(auctionId, viewCount);
                    
                    if (updatedCount > 0) {
                        redisTemplate.opsForHash().put(batchKey, auctionIdStr, viewCount.toString());
                        redisTemplate.opsForSet().remove(DIRTY_FLAG_KEY, auctionIdStr);
                        
                        syncedCount++;
                        log.debug("조회수 DB 동기화 완료: auctionId={}, count={}", auctionIdStr, viewCount);
                    } else {
                        log.warn("조회수 DB 업데이트 실패 (경매 없음?): auctionId={}", auctionIdStr);
                    }
                }
            } catch (Exception e) {
                log.error("조회수 동기화 실패: auctionId={}", auctionIdStr, e);
            }
        }

        return syncedCount;
    }

    public Map<Object, Object> getBatchData(String date) {
        String batchKey = BATCH_KEY_PREFIX + date;
        return redisTemplate.opsForHash().entries(batchKey);
    }

    public void clearCache() {
        Set<String> keys = redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }

        redisTemplate.delete(DIRTY_FLAG_KEY);
        redisTemplate.delete(redisTemplate.keys(BATCH_KEY_PREFIX + "*"));
    }

    public CacheStats getCacheStats() {
        try {
            Long dirtyCount = redisTemplate.opsForSet().size(DIRTY_FLAG_KEY);
            Set<String> viewKeys = redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");

            return new CacheStats(
                    dirtyCount != null ? dirtyCount : 0,
                    viewKeys != null ? viewKeys.size() : 0
            );
        } catch (Exception e) {
            log.error("캐시 통계 조회 실패", e);
            return new CacheStats(0, 0);
        }
    }

    public record CacheStats(long dirtyCount, long cachedAuctionsCount) {
        public String toString() {
            return String.format("더티: %d건, 캐시된 경매: %d건", dirtyCount, cachedAuctionsCount);
        }
    }
}
