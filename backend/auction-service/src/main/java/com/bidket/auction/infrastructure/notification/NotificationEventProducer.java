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

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

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

        StandardEvent event = AuctionClosedEvent.createWinnerEvent(
                winnerId, auctionId, finalPrice, closedAt
        );

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

        StandardEvent event = AuctionClosedEvent.createLoserEvent(
                loserId, auctionId, finalPrice, closedAt
        );

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

        StandardEvent event = OutbidEvent.create(
                previousBidderId, auctionId, currentPrice, outbidAt
        );

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

        StandardEvent event = AuctionReopenedEvent.create(
                auctionId,
                productSizeId,
                previousWinnerId,
                reason,
                newEndTime,
                correlationId
        );

        Map<String, Object> payload = convertToMap(event);

        AuctionOutbox outbox = outboxService.saveNotificationEvent(
                event.eventType(),
                previousWinnerId,
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
        map.put("eventType", event.eventType());
        map.put("occurredAt", event.occurredAt().toString());
        map.put("source", event.source());
        map.put("userId", event.userId() != null ? event.userId().toString() : null);
        map.put("data", event.data());
        return map;
    }
}
