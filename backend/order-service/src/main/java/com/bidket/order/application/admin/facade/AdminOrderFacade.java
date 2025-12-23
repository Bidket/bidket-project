package com.bidket.order.application.admin.facade;

import com.bidket.order.application.admin.info.AdminOrderInfo;
import com.bidket.order.application.admin.info.AdminOrderSummaryInfo;
import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.model.OrderStatus;
import com.bidket.order.domain.order.repository.OrderRepository;
import com.bidket.order.infrastructure.kafka.producer.AuctionEventProducer;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOrderFacade {

    private final OrderRepository orderRepository;
    private final AuctionEventProducer auctionEventProducer;

    public Page<AdminOrderSummaryInfo> getOrders(OrderStatus status, Pageable pageable) {
        Page<Order> page = (status == null)
                ? orderRepository.findAll(pageable)
                : orderRepository.findAllByStatus(status, pageable);

        return page.map(AdminOrderSummaryInfo::from);
    }

    public AdminOrderInfo getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("주문이 존재하지 않습니다."));
        return AdminOrderInfo.from(order);
    }

    @Transactional
    public AdminOrderInfo changeStatus(UUID orderId, OrderStatus status, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("주문이 존재하지 않습니다."));

        Order changed = switch (status) {
            case CANCELED -> order.cancel(LocalDateTime.now());
            case EXPIRED -> order.expire(LocalDateTime.now());
            default -> throw new IllegalStateException("관리자 변경 불가 상태");
        };

        orderRepository.save(changed);

        auctionEventProducer.publishOrderCanceled(
                changed.id(),
                changed.auctionId(),
                changed.userId(),
                reason,
                UUID.randomUUID()
        );

        return AdminOrderInfo.from(changed);
    }
}