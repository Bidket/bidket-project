package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 상회 입찰 알림 이벤트
 * Notification Service로 전달되는 이벤트
 *
 * Event Contract (auction-notification-producer.md 참조):
 * - Topic: notification.auction
 * - eventType: "outbid"
 * - 대상: 이전 최고 입찰자
 *
 * data 필드:
 * - auctionId: 경매 ID
 * - currentPrice: 새로운 최고 입찰 금액
 * - outbidAt: 상회 입찰 발생 시각
 */
public class OutbidEvent {

    private OutbidEvent() {
        throw new UnsupportedOperationException("Factory class");
    }

    /**
     * 상회 입찰 알림 이벤트 생성
     *
     * @param previousBidderId 이전 최고 입찰자 ID (알림 대상)
     * @param auctionId 경매 ID
     * @param currentPrice 새로운 최고 입찰 금액
     * @param outbidAt 상회 입찰 발생 시각
     * @return StandardEvent
     */
    public static StandardEvent create(
            UUID previousBidderId,
            UUID auctionId,
            Long currentPrice,
            LocalDateTime outbidAt
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("auctionId", auctionId.toString());
        data.put("currentPrice", currentPrice);
        data.put("outbidAt", outbidAt.toString());

        return StandardEvent.of(
                "auction-service",
                "outbid",
                previousBidderId,
                data
        );
    }
}
