package com.bidket.auction.infrastructure.client.queue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record QueueStatusResponse(
    @JsonProperty("auctionId") UUID auctionId,
    @JsonProperty("userId") UUID userId,
    @JsonProperty("status") String status,
    @JsonProperty("rank") Long rank,
    @JsonProperty("retryAfter") Integer retryAfter,
    @JsonProperty("message") String message
) {
}
