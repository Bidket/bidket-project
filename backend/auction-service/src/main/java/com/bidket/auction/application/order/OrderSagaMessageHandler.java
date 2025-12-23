package com.bidket.auction.application.order;

import java.util.Map;

public interface OrderSagaMessageHandler {

    void handleOrderCreated(Map<String, Object> payload);

    void handleOrderCreationFailed(Map<String, Object> payload);
}
