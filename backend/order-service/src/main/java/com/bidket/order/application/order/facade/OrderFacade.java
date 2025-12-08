package com.bidket.order.application.order.facade;

import com.bidket.order.application.order.info.OrderInfo;
import com.bidket.order.application.order.info.OrderSummaryInfo;
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
    // TODO 포인트/경매/재고 검증용 Port 추가

    @Transactional
    public OrderInfo createOrder(UUID memberId,
            UUID auctionId,
            UUID shoeId,
            Long amount,
            Long usedPointAmount) {
        // TODO 포인트 잔액 검증, 경매 낙찰 여부/유효 시간 검증, 재고 검증 추가

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
        return page.map(OrderSummaryInfo::from);
    }
}