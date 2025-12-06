package com.bidket.queue.global.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

// TODO KeyGenerator Repository에 적용시키기
@Component
public class KeyGenerator {
    public String configKey(UUID auctionId) {
        return "queue:auction:" + auctionId + ":config";
    }

    public String activeKey(UUID auctionId) {
        return "queue:auction:" + auctionId + ":active";
    }

    public String waitingKey(UUID auctionId) {
        return "queue:auction:" + auctionId + ":waiting";
    }

    public String tokenKey(UUID auctionId) {
        return "queue:token:" + auctionId;
    }
}
