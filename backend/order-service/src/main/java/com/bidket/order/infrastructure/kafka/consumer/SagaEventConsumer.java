package com.bidket.order.infrastructure.kafka.consumer;

import com.bidket.order.application.order.command.CreateOrderFromSagaCommand;
import com.bidket.order.application.order.service.SagaOrderService;
import com.bidket.order.infrastructure.kafka.event.CreateOrderRequestedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Saga Compensation 토픽에서 CREATE_ORDER_REQUESTED 이벤트를 수신하는 Consumer
 * DDD: Infrastructure Layer - 외부 시스템(Kafka)과의 연동
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventConsumer {

    private final SagaOrderService sagaOrderService;
    private final ObjectMapper objectMapper;

    /**
     * CREATE_ORDER_REQUESTED 이벤트 수신
     * Topic: order.auction (Order가 소비, Auction이 생산)
     *
     * @param message JSON 형태의 이벤트 메시지
     */
    @KafkaListener(
            topics = "${order.kafka.topics.order-auction:order.auction}",
            groupId = "${spring.kafka.consumer.group-id:order-service-group}"
    )
    public void handleCreateOrderRequest(String message) {
        try {
            log.info("[SagaEventConsumer] CREATE_ORDER_REQUESTED 수신: message={}", message);

            // JSON → CreateOrderRequestedEvent 변환
            CreateOrderRequestedEvent event = objectMapper.readValue(
                    message,
                    CreateOrderRequestedEvent.class
            );

            // Infrastructure Event → Application Command 변환
            CreateOrderFromSagaCommand command = new CreateOrderFromSagaCommand(
                    event.sagaId(),
                    event.auctionId(),
                    event.userId(),
                    event.productSizeId(),
                    event.price(),
                    event.paymentDeadline()
            );

            // Application Service 호출
            sagaOrderService.createOrderFromSaga(command);

            log.info("[SagaEventConsumer] 주문 생성 완료: sagaId={}", event.sagaId());

        } catch (Exception e) {
            log.error("[SagaEventConsumer] CREATE_ORDER_REQUESTED 처리 실패: message={}, error={}",
                    message, e.getMessage(), e);
            // Exception 발생 시 Kafka는 자동 재시도 (DLQ 처리는 제외 범위)
            throw new RuntimeException("Failed to handle CREATE_ORDER_REQUESTED", e);
        }
    }
}
