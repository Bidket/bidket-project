package com.bidket.auction.infrastructure.client.queue;

import com.bidket.auction.infrastructure.client.queue.dto.QueueStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(
    name = "queue-service",
    url = "${feign.queue-service.url}"
)
public interface QueueClient {

    @GetMapping("/v1/queues/{auctionId}/status")
    QueueStatusResponse checkQueueStatus(
        @PathVariable("auctionId") UUID auctionId,
        @RequestHeader("X-Member-Id") UUID userId
    );
}
