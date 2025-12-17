package com.bidket.auction.application.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class LoggingAuctionEventProcessor implements AuctionEventProcessor {

    @Override
    public void process(String eventType, Map<String, Object> payload) {
        log.info("수신 이벤트 처리: eventType={}, payload={}", eventType, payload);
    }
}
