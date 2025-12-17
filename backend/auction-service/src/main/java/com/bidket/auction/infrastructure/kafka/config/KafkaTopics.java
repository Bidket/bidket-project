package com.bidket.auction.infrastructure.kafka.config;

/**
 * Kafka 토픽 명명 규칙: [소비_도메인].[생산_도메인]
 *
 * 예시:
 * - auction.order: Auction 서비스가 소비, Order 서비스가 생산
 * - order.auction: Order 서비스가 소비, Auction 서비스가 생산
 */
public final class KafkaTopics {

    private KafkaTopics() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Order 서비스가 생산 → Auction 서비스가 소비
    public static final String AUCTION_ORDER = "auction.order";

    // Auction 서비스가 생산 → Order 서비스가 소비
    public static final String ORDER_AUCTION = "order.auction";

    // Auction 서비스 내부 이벤트 또는 다른 서비스가 소비
    public static final String AUCTION_EVENTS = "auction.events";

    public static final String QUEUE_EVENTS = "queue.events";
    public static final String PRODUCT_EVENTS = "product.events";
}
