package com.bidket.order.application.order.scheduler;

import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.repository.OrderRepository;
import com.bidket.order.infrastructure.kafka.producer.AuctionEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 결제 타임아웃 스케줄러
 *
 * 주기적으로 결제 시간이 초과된 주문을 조회하여 EXPIRED 상태로 변경하고,
 * Auction Service에 PAYMENT_TIMEOUT 이벤트를 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {

    private final OrderRepository orderRepository;
    private final AuctionEventProducer auctionEventProducer;
    private final Clock clock;

    /**
     * 1분마다 결제 시간 초과된 주문 처리
     */
    @Scheduled(fixedDelay = 60000) // 1분마다
    @Transactional
    public void processExpiredOrders() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Order> expiredOrders = orderRepository.findExpiredOrders(now);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("[PaymentTimeoutScheduler] 결제 시간 초과된 주문 {} 건 발견", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                // 1. 주문 상태를 EXPIRED로 변경
                Order expiredOrder = order.expire(now);
                orderRepository.save(expiredOrder);

                // 2. PAYMENT_TIMEOUT 이벤트 발행 (OutBox 패턴)
                auctionEventProducer.publishPaymentTimeout(
                        expiredOrder.id(),
                        expiredOrder.auctionId(),
                        expiredOrder.userId(),
                        "결제 시간 초과",
                        UUID.randomUUID() // correlationId 생성
                );

                log.info("[PaymentTimeoutScheduler] 주문 만료 처리 완료: orderId={}, auctionId={}",
                        expiredOrder.id(), expiredOrder.auctionId());

            } catch (Exception e) {
                log.error("[PaymentTimeoutScheduler] 주문 만료 처리 실패: orderId={}, auctionId={}",
                        order.id(), order.auctionId(), e);
                // 다음 배치에서 재시도하도록 예외를 삼킴
            }
        }
    }
}
