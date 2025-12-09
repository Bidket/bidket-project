package com.bidket.order.application.facade;

import com.bidket.order.application.info.OrderInfo;
import com.bidket.order.domain.model.Order;
import com.bidket.order.domain.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
}