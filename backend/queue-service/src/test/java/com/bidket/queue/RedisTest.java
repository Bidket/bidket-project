package com.bidket.queue;

import com.bidket.queue.infrastructure.redis.RedisRepository;
import com.bidket.queue.infrastructure.redis.RedisUtils;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.UUID;

@Import(RedisUtils.class)
@SpringBootTest
public class RedisTest {

    private static final Logger log = LoggerFactory.getLogger(RedisTest.class);
    @Autowired
    private RedisRepository redisRepository;

    @Test
    void queueConfig() {
        QueueCreateRequest request = QueueCreateRequest.builder()
                .auctionId(UUID.randomUUID())
                .maxActive(100L)
                .permitsPerSec(1)
                .openAt(LocalDateTime.now())
                .closeAt(LocalDateTime.now().plusHours(5L))
                .build();

        redisRepository.createQueueConfig(request).block();

        String key = "auction:config:" + request.auctionId() + ":config";

        log.info("value: " + redisRepository.getValue(key));
    }
}
