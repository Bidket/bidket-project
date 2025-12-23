package com.bidket.order.application.outbox.publisher;

import com.bidket.order.domain.outbox.model.OrderOutbox;

/**
 * OutBox 이벤트를 외부(Kafka)로 발행하는 인터페이스
 */
public interface OutboxEventPublisher {

    /**
     * OutBox 이벤트를 Kafka로 발행
     *
     * @param outbox 발행할 OutBox
     * @throws RuntimeException Kafka 발행 실패 시
     */
    void publish(OrderOutbox outbox);
}
