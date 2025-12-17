package com.bidket.auction.application.kafka;

import java.util.Map;

public interface AuctionEventProcessor {

    void process(String eventType, Map<String, Object> payload);
}
