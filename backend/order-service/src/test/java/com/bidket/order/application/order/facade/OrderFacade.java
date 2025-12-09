package com.bidket.order.application.order.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import com.bidket.order.application.order.info.OrderInfo;
import com.bidket.order.application.order.info.OrderSummaryInfo;
import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.model.OrderStatus;
import com.bidket.order.domain.order.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderFacade orderFacade;

    @Test
    @DisplayName("주문 생성 성공 - OrderInfo를 반환한다")
    void createOrder_success() {
        // given
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID auctionId = UUID.fromString("7c4d3f9a-1b2c-4d5e-9f01-23456789abcd");
        UUID shoeId = UUID.fromString("b1a2c3d4-e5f6-7890-abcd-ef0123456789");
        UUID orderId = UUID.fromString("d9a28926-8f22-49bb-8044-a51b83074e50");

        long amount = 250000L;
        long usedPointAmount = 10000L;

        LocalDateTime now = LocalDateTime.now();

        Order savedOrder = Order.of(
                orderId,
                userId,
                auctionId,
                shoeId,
                OrderStatus.PAYMENT,
                amount,
                usedPointAmount,
                now.plusMinutes(15),
                now,
                now
        );

        given(orderRepository.save(org.mockito.ArgumentMatchers.any(Order.class)))
                .willReturn(savedOrder);

        // when
        OrderInfo result = orderFacade.createOrder(
                userId,
                auctionId,
                shoeId,
                amount,
                usedPointAmount
        );

        // then
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAuctionId()).isEqualTo(auctionId);
        assertThat(result.getShoeId()).isEqualTo(shoeId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getUsedPointAmount()).isEqualTo(usedPointAmount);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT);
    }

    @Test
    @DisplayName("주문 생성 실패 - Repository에서 예외 발생 시 예외를 전파한다")
    void createOrder_fail_whenRepositoryThrows() {
        // given
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID auctionId = UUID.fromString("7c4d3f9a-1b2c-4d5e-9f01-23456789abcd");
        UUID shoeId = UUID.fromString("b1a2c3d4-e5f6-7890-abcd-ef0123456789");

        long amount = 250000L;
        long usedPointAmount = 10000L;

        doThrow(new RuntimeException("DB error"))
                .when(orderRepository)
                .save(org.mockito.ArgumentMatchers.any(Order.class));

        // when & then
        assertThrows(RuntimeException.class, () ->
                orderFacade.createOrder(
                        userId,
                        auctionId,
                        shoeId,
                        amount,
                        usedPointAmount
                )
        );
    }

    @Test
    @DisplayName("주문 목록 조회 성공 - 사용자 주문 목록을 페이징으로 반환한다")
    void getOrders_success() {
        // given
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID auctionId = UUID.fromString("7c4d3f9a-1b2c-4d5e-9f01-23456789abcd");
        UUID shoeId = UUID.fromString("b1a2c3d4-e5f6-7890-abcd-ef0123456789");
        UUID orderId = UUID.fromString("d9a28926-8f22-49bb-8044-a51b83074e50");

        LocalDateTime now = LocalDateTime.now();

        Order order = Order.of(
                orderId,
                userId,
                auctionId,
                shoeId,
                OrderStatus.PAYMENT,
                250_000L,
                10_000L,
                now.plusMinutes(15),
                now,
                now
        );

        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

        given(orderRepository.findByUserId(userId, pageable))
                .willReturn(orderPage);

        // when
        Page<OrderSummaryInfo> result = orderFacade.getOrders(userId, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        OrderSummaryInfo info = result.getContent().get(0);

        assertThat(info.getOrderId()).isEqualTo(orderId);
        assertThat(info.getStatus()).isEqualTo(OrderStatus.PAYMENT);
        assertThat(info.getAmount()).isEqualTo(250_000L);
    }

    @Test
    @DisplayName("주문 목록 조회 실패 - Repository에서 예외 발생 시 예외를 전파한다")
    void getOrders_fail_whenRepositoryThrows() {
        // given
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        Pageable pageable = PageRequest.of(0, 20);

        doThrow(new RuntimeException("DB error"))
                .when(orderRepository)
                .findByUserId(userId, pageable);

        // when & then
        assertThrows(RuntimeException.class, () ->
                orderFacade.getOrders(userId, pageable)
        );
    }
}