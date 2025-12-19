package com.bidket.auction.infrastructure.notification;

import com.bidket.auction.application.outbox.service.OutboxService;
import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.bidket.auction.infrastructure.kafka.event.AuctionClosedEvent;
import com.bidket.auction.infrastructure.kafka.event.AuctionReopenedEvent;
import com.bidket.auction.infrastructure.kafka.event.OutbidEvent;
import com.bidket.auction.infrastructure.kafka.event.StandardEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Notification Service 연동을 위한 이벤트 Producer
 * OutBox 패턴을 사용하여 트랜잭션 커밋 후 이벤트 발행 보장
 *
 * INT-005 (Notification Service 연동) 구현
 *
 * 발행 가능한 이벤트:
 * 1. 경매 종료 알림 (AUCTION_CLOSED)
 *    - 낙찰자: result="WON"
 *    - 패찰자: result="LOST"
 * 2. 상회 입찰 알림 (OUTBID)
 *    - 이전 최고 입찰자에게 발행
 * 3. 경매 재오픈 알림 (AUCTION_REOPENED)
 *    - 결제 타임아웃 또는 주문 취소로 경매가 재오픈되었을 때 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    /**
     * 낙찰자 알림 이벤트를 OutBox에 저장
     *
     * @param winnerId 낙찰자 ID
     * @param auctionId 경매 ID
     * @param finalPrice 낙찰 금액
     * @param closedAt 경매 종료 시각
     * @param correlationId 상관 ID (Saga ID)
     * @return 저장된 OutBox 엔티티
     */
    @Transactional
    public AuctionOutbox publishWinnerNotification(
            UUID winnerId,
            UUID auctionId,
            Long finalPrice,
            LocalDateTime closedAt,
            UUID correlationId
    ) {
        log.info("낙찰자 알림 이벤트 발행: winnerId={}, auctionId={}, finalPrice={}",
                winnerId, auctionId, finalPrice);

        // 낙찰자 알림 이벤트 생성
        StandardEvent event = AuctionClosedEvent.createWinnerEvent(
                winnerId, auctionId, finalPrice, closedAt
        );

        // StandardEvent를 Map으로 변환하여 OutBox에 저장
        Map<String, Object> payload = convertToMap(event);

        AuctionOutbox outbox = outboxService.saveNotificationEvent(
                event.eventType(),
                winnerId,
                payload,
                correlationId
        );

        log.info("낙찰자 알림 OutBox 저장 완료: outboxId={}, winnerId={}, auctionId={}",
                outbox.getId(), winnerId, auctionId);
        return outbox;
    }

    /**
     * 패찰자 알림 이벤트를 OutBox에 저장
     *
     * @param loserId 패찰자 ID
     * @param auctionId 경매 ID
     * @param finalPrice 낙찰 금액
     * @param closedAt 경매 종료 시각
     * @param correlationId 상관 ID (Saga ID)
     * @return 저장된 OutBox 엔티티
     */
    @Transactional
    public AuctionOutbox publishLoserNotification(
            UUID loserId,
            UUID auctionId,
            Long finalPrice,
            LocalDateTime closedAt,
            UUID correlationId
    ) {
        log.info("패찰자 알림 이벤트 발행: loserId={}, auctionId={}, finalPrice={}",
                loserId, auctionId, finalPrice);

        // 패찰자 알림 이벤트 생성
        StandardEvent event = AuctionClosedEvent.createLoserEvent(
                loserId, auctionId, finalPrice, closedAt
        );

        // StandardEvent를 Map으로 변환하여 OutBox에 저장
        Map<String, Object> payload = convertToMap(event);

        AuctionOutbox outbox = outboxService.saveNotificationEvent(
                event.eventType(),
                loserId,
                payload,
                correlationId
        );

        log.info("패찰자 알림 OutBox 저장 완료: outboxId={}, loserId={}, auctionId={}",
                outbox.getId(), loserId, auctionId);
        return outbox;
    }

    /**
     * 상회 입찰 알림 이벤트를 OutBox에 저장
     *
     * @param previousBidderId 이전 최고 입찰자 ID (알림 대상)
     * @param auctionId 경매 ID
     * @param currentPrice 새로운 최고 입찰 금액
     * @param outbidAt 상회 입찰 발생 시각
     * @param correlationId 상관 ID (입찰 ID 또는 경매 ID)
     * @return 저장된 OutBox 엔티티
     */
    @Transactional
    public AuctionOutbox publishOutbidNotification(
            UUID previousBidderId,
            UUID auctionId,
            Long currentPrice,
            LocalDateTime outbidAt,
            UUID correlationId
    ) {
        log.info("상회 입찰 알림 이벤트 발행: previousBidderId={}, auctionId={}, currentPrice={}",
                previousBidderId, auctionId, currentPrice);

        // 상회 입찰 알림 이벤트 생성
        StandardEvent event = OutbidEvent.create(
                previousBidderId, auctionId, currentPrice, outbidAt
        );

        // StandardEvent를 Map으로 변환하여 OutBox에 저장
        Map<String, Object> payload = convertToMap(event);

        AuctionOutbox outbox = outboxService.saveNotificationEvent(
                event.eventType(),
                previousBidderId,
                payload,
                correlationId
        );

        log.info("상회 입찰 알림 OutBox 저장 완료: outboxId={}, previousBidderId={}, auctionId={}",
                outbox.getId(), previousBidderId, auctionId);
        return outbox;
    }

    /**
     * 경매 재오픈 알림 이벤트를 OutBox에 저장
     *
     * @param auctionId 경매 ID
     * @param productSizeId 상품 사이즈 ID
     * @param previousWinnerId 이전 낙찰자 ID
     * @param reason 재오픈 사유
     * @param newEndTime 새로운 종료 시간
     * @param correlationId 상관 ID (Saga ID)
     * @return 저장된 OutBox 엔티티
     */
    @Transactional
    public AuctionOutbox publishAuctionReopenedNotification(
            UUID auctionId,
            UUID productSizeId,
            UUID previousWinnerId,
            String reason,
            LocalDateTime newEndTime,
            UUID correlationId
    ) {
        log.info("경매 재오픈 알림 이벤트 발행: auctionId={}, previousWinnerId={}, reason={}",
                auctionId, previousWinnerId, reason);

        // 경매 재오픈 알림 이벤트 생성
        StandardEvent event = AuctionReopenedEvent.create(
                auctionId,
                productSizeId,
                previousWinnerId,
                reason,
                newEndTime,
                correlationId
        );

        // StandardEvent를 Map으로 변환하여 OutBox에 저장
        Map<String, Object> payload = convertToMap(event);

        AuctionOutbox outbox = outboxService.saveNotificationEvent(
                event.eventType(),
                auctionId,
                payload,
                correlationId
        );

        log.info("경매 재오픈 알림 OutBox 저장 완료: outboxId={}, auctionId={}, previousWinnerId={}",
                outbox.getId(), auctionId, previousWinnerId);
        return outbox;
    }

    private Map<String, Object> convertToMap(StandardEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventId", event.eventId().toString());
        map.put("occurredAt", event.occurredAt().toString());
        map.put("source", event.source());
        map.put("eventType", event.eventType());
        map.put("userId", event.userId() != null ? event.userId().toString() : null);
        map.put("data", event.data());
        return map;
    }
}
