package com.bidket.auction.application.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnMissingBean(OrderSagaMessageHandler.class)
public class LoggingOrderSagaMessageHandler implements OrderSagaMessageHandler {

    @Override
    public void handleOrderCreated(Map<String, Object> payload) {
        log.info("[LoggingOrderSagaMessageHandler] ORDER_CREATED 수신: payload={}", payload);

    }

    @Override
    public void handleOrderCreationFailed(Map<String, Object> payload) {
        log.warn("[LoggingOrderSagaMessageHandler] ORDER_CREATION_FAILED 수신: payload={}", payload);

    }
}
