package com.bidket.queue.infrastructure.redis;

import com.bidket.queue.domain.repository.QueueTrafficRepository;
import com.bidket.queue.global.util.KeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

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

        long now = System.currentTimeMillis();

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
    public Mono<Boolean> addWaitingUser(String waitingKey, UUID userId) {
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
    public Mono<Long> getRank(String waitingKey, UUID userId) {
        return redisOps.opsForZSet().rank(waitingKey, userId);
    }

    @Override
    public Mono<Boolean> saveToken(String tokenKey, Map<UUID, String> tokens) {
        return redisOps.opsForHash()
                .putAll(tokenKey, tokens);
    }

    @Override
    public Mono<String> getToken(String tokenKey, UUID userId) {
        return redisOps.opsForHash()
                .get(tokenKey, userId)
                .map(String::valueOf);
    }
}
