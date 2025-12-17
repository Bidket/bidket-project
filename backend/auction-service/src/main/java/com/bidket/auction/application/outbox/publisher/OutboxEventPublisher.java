package com.bidket.auction.application.outbox.publisher;

import com.bidket.auction.domain.outbox.model.AuctionOutbox;

public interface OutboxEventPublisher {

    void publish(AuctionOutbox outbox);
}
