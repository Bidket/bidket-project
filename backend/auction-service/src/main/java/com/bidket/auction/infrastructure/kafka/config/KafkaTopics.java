package com.bidket.auction.infrastructure.kafka.config;

public final class KafkaTopics {

    private KafkaTopics() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String AUCTION_ORDER = "auction.order";

    public static final String ORDER_AUCTION = "order.auction";

    public static final String AUCTION_EVENTS = "auction.events";

    public static final String QUEUE_EVENTS = "queue.events";
    public static final String PRODUCT_EVENTS = "product.events";

    public static final String NOTIFICATION_AUCTION = "notification.auction";
}
