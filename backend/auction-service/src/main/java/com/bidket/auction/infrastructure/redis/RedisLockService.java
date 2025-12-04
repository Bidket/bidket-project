package com.bidket.auction.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_PREFIX = "lock:";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    public String acquireLock(String key, Duration timeout) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, timeout);
        
        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Lock acquired: {}", lockKey);
            return lockValue;
        }
        
        log.debug("Failed to acquire lock: {}", lockKey);
        return null;
    }

    public boolean releaseLock(String key, String lockValue) {
        String lockKey = LOCK_PREFIX + key;
        
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(
                script, Long.class
            ),
            Collections.singletonList(lockKey),
            lockValue
        );
        
        boolean released = result != null && result > 0;
        if (released) {
            log.debug("Lock released: {}", lockKey);
        } else {
            log.warn("Failed to release lock: {} (lock value mismatch)", lockKey);
        }
        
        return released;
    }

    public String acquireLock(String key) {
        return acquireLock(key, DEFAULT_TIMEOUT);
    }
}

