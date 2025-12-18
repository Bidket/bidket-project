package com.bidket.order.application.order.facade;

import com.bidket.order.application.order.info.OrderInfo;
import com.bidket.order.application.order.info.OrderSummaryInfo;
import com.bidket.order.application.order.port.AuctionQueryPort;
import com.bidket.order.application.order.port.AuctionSnapshot;
import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.repository.OrderRepository;
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
    private final AuctionQueryPort auctionQueryPort;
    // TODO: 포인트/재고 검증용 Port 추가

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
        // TODO: 경매 낙찰 상태 + 낙찰자 검증 추가
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

    public Page<OrderSummaryInfo> getOrders(UUID userId, Pageable pageable) {
        Page<Order> page = orderRepository.findByUserId(userId, pageable);

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

    @Transactional
    public void deleteOrder(UUID userId, UUID orderId) {
        orderRepository.softDelete(orderId, userId);
    }
}