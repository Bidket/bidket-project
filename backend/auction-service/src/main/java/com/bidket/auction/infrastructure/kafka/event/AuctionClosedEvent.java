package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 경매 종료 알림 이벤트
 * Notification Service로 전달되는 이벤트
 *
 * Event Contract (auction-notification-producer.md 참조):
 * - Topic: notification.auction
 * - eventType: "closed"
 * - 대상:
 *   - 낙찰자: result="WON"
 *   - 패찰자: result="LOST"
 *
 * data 필드:
 * - auctionId: 경매 ID
 * - result: 낙찰/패찰 여부 (WON/LOST)
 * - finalPrice: 낙찰 금액
 * - closedAt: 경매 종료 시각
 */
public class AuctionClosedEvent {

    private AuctionClosedEvent() {
        throw new UnsupportedOperationException("Factory class");
    }

    /**
     * 낙찰자 알림 이벤트 생성
     *
     * @param winnerId 낙찰자 ID
     * @param auctionId 경매 ID
     * @param finalPrice 낙찰 금액
     * @param closedAt 경매 종료 시각
     * @return StandardEvent
     */
    public static StandardEvent createWinnerEvent(
            UUID winnerId,
            UUID auctionId,
            Long finalPrice,
            LocalDateTime closedAt
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("auctionId", auctionId.toString());
        data.put("result", "WON");
        data.put("finalPrice", finalPrice);
        data.put("closedAt", closedAt.toString());

        return StandardEvent.of(
                "auction-service",
                "closed",
                winnerId,
                data
        );
    }

    /**
     * 패찰자 알림 이벤트 생성
     *
     * @param loserId 패찰자 ID
     * @param auctionId 경매 ID
     * @param finalPrice 낙찰 금액
     * @param closedAt 경매 종료 시각
     * @return StandardEvent
     */
    public static StandardEvent createLoserEvent(
            UUID loserId,
            UUID auctionId,
            Long finalPrice,
            LocalDateTime closedAt
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("auctionId", auctionId.toString());
        data.put("result", "LOST");
        data.put("finalPrice", finalPrice);
        data.put("closedAt", closedAt.toString());

        return StandardEvent.of(
                "auction-service",
                "closed",
                loserId,
                data
        );
    }
}
