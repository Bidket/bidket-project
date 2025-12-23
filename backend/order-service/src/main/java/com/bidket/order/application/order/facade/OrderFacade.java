package com.bidket.order.application.order.facade;

import com.bidket.order.application.order.info.OrderInfo;
import com.bidket.order.application.order.info.OrderSummaryInfo;
import com.bidket.order.application.order.port.AuctionQueryPort;
import com.bidket.order.application.order.port.AuctionSnapshot;
import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.model.OrderStatus;
import com.bidket.order.domain.order.repository.OrderRepository;
import com.bidket.order.infrastructure.kafka.producer.AuctionEventProducer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderRepository orderRepository;
    private final AuctionEventProducer auctionEventProducer;
    private final Clock clock;
    private final AuctionQueryPort auctionQueryPort;

    @Transactional
    public OrderInfo createOrder(
            UUID memberId,
            UUID auctionId,
            UUID shoeId,
            Long amount,
            Long usedPointAmount
    ) {
        // TODO: 포인트/재고 검증 추가
        if (orderRepository.existsByAuctionId(auctionId)) {
            throw new IllegalStateException("이미 주문이 생성된 경매입니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime paymentExpiredAt = now.plusMinutes(15);

        Order order = Order.createForPayment(
                memberId,
                auctionId,
                shoeId,
                amount,
                usedPointAmount,
                paymentExpiredAt,
                now
        );

        Order saved = orderRepository.save(order);
        return OrderInfo.from(saved);
    }

    /**
     * Auction Service로부터의 이벤트 기반 주문 생성 Kafka Consumer에서 호출됩니다.
     */
    @Transactional
    public OrderInfo createOrderFromAuction(
            UUID userId,
            UUID auctionId,
            UUID productSizeId,
            Long price,
            Long usedPointAmount,
            LocalDateTime paymentDeadline,
            UUID sagaId,
            UUID correlationId
    ) {
        LocalDateTime now = LocalDateTime.now();

        Order order = Order.createForPayment(
                userId,
                auctionId,
                productSizeId,
                price,
                usedPointAmount,
                paymentDeadline,
                now
        );

        Order saved = orderRepository.save(order);

        return OrderInfo.from(saved);
    }

    public Page<OrderSummaryInfo> getOrders(UUID userId, OrderStatus status, Pageable pageable) {
        Page<Order> page = (status == null)
                ? orderRepository.findByUserId(userId, pageable)
                : orderRepository.findByUserIdAndStatus(userId, status, pageable);

        return page.map(order -> {
            try {
                AuctionSnapshot auction = auctionQueryPort.getAuction(order.auctionId());
                return OrderSummaryInfo.from(order, auction);
            } catch (Exception e) {
                return OrderSummaryInfo.from(order);
            }
        });
    }

    public OrderInfo getOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("주문을 찾을 수 없습니다."));

        if (!order.userId().equals(userId)) {
            throw new IllegalStateException("본인의 주문만 조회할 수 있습니다.");
        }

        return OrderInfo.from(order);
    }

    public OrderStatus getOrderStatus(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("주문을 찾을 수 없습니다."));

        if (!order.userId().equals(userId)) {
            throw new IllegalStateException("본인의 주문만 조회할 수 있습니다.");
        }

        return order.status();
    }

    @Transactional
    public void deleteOrder(UUID userId, UUID orderId) {
        orderRepository.softDelete(orderId, userId);
    }

    /**
     * 주문 취소 PAYMENT 상태의 주문만 취소 가능
     */
    @Transactional
    public OrderInfo cancelOrder(UUID orderId, UUID userId) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        // 2. 소유자 확인
        if (!order.userId().equals(userId)) {
            throw new IllegalArgumentException(
                    "주문 취소 권한이 없습니다: orderId=" + orderId + ", userId=" + userId);
        }

        // 3. 주문 취소 (PAYMENT → CANCELED)
        LocalDateTime now = LocalDateTime.now(clock);
        Order canceledOrder = order.cancel(now);
        Order saved = orderRepository.save(canceledOrder);

        // 4. ORDER_CANCELED 이벤트 발행 (OutBox 패턴)
        auctionEventProducer.publishOrderCanceled(
                saved.id(),
                saved.auctionId(),
                saved.userId(),
                "사용자 요청에 의한 주문 취소",
                UUID.randomUUID() // correlationId 생성
        );

        return OrderInfo.from(saved);
    }
}