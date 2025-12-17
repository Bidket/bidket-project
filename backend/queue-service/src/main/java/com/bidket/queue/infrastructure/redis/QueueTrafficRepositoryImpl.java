package com.bidket.queue.infrastructure.redis;

import com.bidket.queue.domain.repository.QueueTrafficRepository;
import com.bidket.queue.global.util.KeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class QueueTrafficRepositoryImpl implements QueueTrafficRepository {
    private final ReactiveRedisOperations<String, Object> redisOps;
    private final KeyGenerator keyGenerator;

    @Override
    public Mono<Long> addAllActiveUser(String activeKey, List<UUID> userIds) {
        if (userIds.isEmpty())
            return Mono.just(0L);

        long now = Instant.now()
                .plus(5, ChronoUnit.MINUTES)
                .getEpochSecond();

        Set<ZSetOperations.TypedTuple<Object>> tuples = userIds.stream()
                .map(id -> ZSetOperations.TypedTuple.of((Object) id.toString(), (double) now))
                .collect(Collectors.toSet());

        return redisOps.opsForZSet()
                .addAll(activeKey, tuples);
    }

    @Override
    public Mono<Long> getActiveUserCount(UUID auctionId) {
        String key = keyGenerator.activeKey(auctionId);
        return redisOps.opsForZSet()
                .size(key);
    }

    @Override
    public Flux<UUID> getExpiredActiveUser(UUID auctionId) {
        String key = keyGenerator.activeKey(auctionId);
        double now = Instant.now().getEpochSecond();
        return redisOps.opsForZSet()
                .rangeByScore(key, Range.closed(0.0, now))
                .map(userId -> UUID.fromString(userId.toString()));

    }

    @Override
    public Mono<Boolean> renewActiveUser(UUID auctionId, UUID userId, Instant updateTime) {
        String activeKey = keyGenerator.activeKey(auctionId);

        return redisOps.opsForZSet()
                .add(activeKey, userId.toString(), updateTime.getEpochSecond());
    }

    @Override
    public Mono<Long> removeActiveUsers(UUID auctionId, List<UUID> userIds) {
        if (userIds.isEmpty())
            return Mono.empty();

        String activeKey = keyGenerator.activeKey(auctionId);
        String tokenKey = keyGenerator.tokenKey(auctionId);

        Object[] userIdArray = userIds.stream()
                .map(UUID::toString)
                .toArray();

        return redisOps.opsForZSet()
                .remove(activeKey, userIdArray)
                .flatMap(deleted ->
                        redisOps.opsForHash()
                                .remove(tokenKey, userIdArray)
                );
    }

    @Override
    public Mono<Boolean> deleteActiveQueue(UUID auctionId) {
        String key = keyGenerator.activeKey(auctionId);
        return redisOps.opsForZSet()
                .delete(key);
    }

    @Override
    public Mono<Boolean> addWaitingUser(UUID auctionId, UUID userId) {
        String waitingKey = "queue:auction:" + auctionId + ":waiting";
        long now = System.currentTimeMillis();
        // TODO waiting queue 용량 제한
        return redisOps.opsForZSet().add(waitingKey, userId, now);
    }

    @Override
    public Mono<Long> getWaitingUserCount(UUID auctionId) {
        String key = keyGenerator.waitingKey(auctionId);

        return redisOps.opsForZSet()
                .size(key);
    }

    @Override
    public Mono<Long> removeWaitingUser(String waitingKey, UUID userId) {
        return redisOps.opsForZSet()
                .remove(waitingKey, userId);
    }

    @Override
    public Mono<List<UUID>> popUserIdWaitingQueue(String waitingKey, long limit) {
        return redisOps.opsForZSet()
                .popMin(waitingKey, limit)
                .map(ZSetOperations.TypedTuple::getValue)
                .map(uuid -> UUID.fromString((String) uuid))
                .collectList();
    }

    @Override
    public Mono<Boolean> deleteWaitingQueue(UUID auctionId) {
        String key = keyGenerator.waitingKey(auctionId);
        return redisOps.opsForZSet()
                .delete(key);
    }

    @Override
    public Mono<Long> getRank(UUID auctionId, UUID userId) {
        String waitingKey = "queue:auction:" + auctionId + ":waiting";
        return redisOps.opsForZSet().rank(waitingKey, userId);
    }

    @Override
    public Mono<Boolean> saveToken(UUID auctionId, Map<UUID, String> tokens) {
        String tokenKey = "queue:token:" + auctionId;
        return redisOps.opsForHash()
                .putAll(tokenKey, tokens);
    }

    @Override
    public Mono<String> getToken(UUID userId, UUID auctionId) {
        String tokenKey = "queue:token:" + auctionId;
        return redisOps.opsForHash()
                .get(tokenKey, userId)
                .map(String::valueOf);
    }
}
