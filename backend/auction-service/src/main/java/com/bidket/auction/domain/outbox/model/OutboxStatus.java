package com.bidket.auction.domain.outbox.model;

public enum OutboxStatus {
    PENDING,
    PUBLISHING,
    PUBLISHED,
    FAILED
}
