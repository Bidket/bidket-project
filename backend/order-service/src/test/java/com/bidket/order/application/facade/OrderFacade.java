package com.bidket.order.application.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bidket.order.application.info.OrderInfo;
import com.bidket.order.domain.model.Order;
import com.bidket.order.domain.repository.OrderRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @InjectMocks
    private OrderFacade orderFacade;

    @Mock
    private OrderRepository orderRepository;

    @Test
    @DisplayName("주문 생성 성공 - 주문 저장 후 생성 정보 반환")
    void createOrder_success() {
        // given
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID auctionId = UUID.fromString("7c4d3f9a-1b2c-4d5e-9f01-23456789abcd");
        UUID shoeId = UUID.fromString("b1a2c3d4-e5f6-7890-abcd-ef0123456789");
        long amount = 250_000L;
        long usedPointAmount = 10_000L;

        // Facade 내부에서 Order.createForPayment(...)로 생성하고
        // Repository.save(...) 결과를 기반으로 OrderInfo를 반환한다고 가정
        Order savedOrder = org.mockito.Mockito.mock(Order.class);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        OrderInfo result = orderFacade.createOrder(
                userId,
                auctionId,
                shoeId,
                amount,
                usedPointAmount
        );

        // then
        assertThat(result).isNotNull();
        then(orderRepository)
                .should()
                .save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 실패 - 저장 과정에서 예외 발생 시 예외 전파")
    void createOrder_fail_whenRepositoryThrows() {
        // given
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID auctionId = UUID.fromString("7c4d3f9a-1b2c-4d5e-9f01-23456789abcd");
        UUID shoeId = UUID.fromString("b1a2c3d4-e5f6-7890-abcd-ef0123456789");
        long amount = 250_000L;
        long usedPointAmount = 10_000L;

        given(orderRepository.save(any(Order.class)))
                .willThrow(new RuntimeException("DB error"));

        // when & then
        assertThatThrownBy(() ->
                orderFacade.createOrder(
                        userId,
                        auctionId,
                        shoeId,
                        amount,
                        usedPointAmount
                )
        ).isInstanceOf(RuntimeException.class);

        then(orderRepository)
                .should()
                .save(any(Order.class));
    }
}