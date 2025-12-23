package com.bidket.auction.application.order;

import java.util.Map;

public interface OrderEventProcessor {

    void process(String eventType, Map<String, Object> payload);
}
