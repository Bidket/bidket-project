package com.bidket.queue;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.queue.presentation.api.QueueController;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest
public class RedisTest {

    private static final Logger log = LoggerFactory.getLogger(RedisTest.class);
    @Autowired
    private QueueController queueController;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void queueConfig() throws JsonProcessingException {
        QueueCreateRequest request = QueueCreateRequest.builder()
                .auctionId(UUID.randomUUID())
                .maxActive(100L)
                .permitsPerSec(1)
                .openAt(LocalDateTime.now())
                .closeAt(LocalDateTime.now().plusHours(5L))
                .build();

        ResponseEntity<ApiResponse<QueueCreateResponse>> response = queueController.createQueueConfig(request).block();

        String key = "auction:config:" + request.auctionId() + ":config";

        log.info("response: \n" + mapper.writeValueAsString(response));
    }
}
