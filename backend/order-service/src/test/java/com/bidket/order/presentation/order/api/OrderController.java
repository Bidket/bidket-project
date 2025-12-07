package com.bidket.order.presentation.order.api;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidket.order.application.order.facade.OrderFacade;
import com.bidket.order.application.order.info.OrderInfo;
import com.bidket.order.application.order.info.OrderSummaryInfo;
import com.bidket.order.domain.order.model.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderFacade orderFacade;

    @Test
    @DisplayName("주문 생성 성공 - 200 OK와 ApiResponse 반환")
    void createOrder_success() throws Exception {
        // given
        String requestJson = """
                {
                  "userId": "11111111-2222-3333-4444-555555555555",
                  "auctionId": "7c4d3f9a-1b2c-4d5e-9f01-23456789abcd",
                  "shoeId": "b1a2c3d4-e5f6-7890-abcd-ef0123456789",
                  "amount": 250000,
                  "usePointAmount": 10000
                }
                """;

        UUID orderId = UUID.fromString("d9a28926-8f22-49bb-8044-a51b83074e50");
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID auctionId = UUID.fromString("7c4d3f9a-1b2c-4d5e-9f01-23456789abcd");
        UUID shoeId = UUID.fromString("b1a2c3d4-e5f6-7890-abcd-ef0123456789");

        long amount = 250000L;
        long usedPointAmount = 10000L;

        LocalDateTime now = LocalDateTime.now();

        OrderInfo orderInfo = new OrderInfo(
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

        given(orderFacade.createOrder(
                any(UUID.class),
                any(UUID.class),
                any(UUID.class),
                anyLong(),
                anyLong()
        )).willReturn(orderInfo);

        // when & then
        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("신발 경매 주문이 생성되었습니다.")))
                .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.auctionId").value(auctionId.toString()))
                .andExpect(jsonPath("$.data.shoeId").value(shoeId.toString()))
                .andExpect(jsonPath("$.data.amount").value((int) amount))
                .andExpect(jsonPath("$.data.usedPointAmount").value((int) usedPointAmount));
    }

    @Test
    @DisplayName("주문 생성 실패 - 내부 오류 발생 시 예외 전파")
    void createOrder_fail_whenInternalErrorOccurs() throws Exception {
        // given
        String requestJson = """
                {
                  "userId": "11111111-2222-3333-4444-555555555555",
                  "auctionId": "7c4d3f9a-1b2c-4d5e-9f01-23456789abcd",
                  "shoeId": "b1a2c3d4-e5f6-7890-abcd-ef0123456789",
                  "amount": 250000,
                  "usePointAmount": 10000
                }
                """;

        doThrow(new RuntimeException("internal error"))
                .when(orderFacade)
                .createOrder(
                        any(UUID.class),
                        any(UUID.class),
                        any(UUID.class),
                        anyLong(),
                        anyLong()
                );

        // when & then
        ServletException ex = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                        .andReturn()
        );
    }

    @Test
    @DisplayName("주문 목록 조회 성공 - 200 OK와 PageResponse 반환")
    void getOrders_success() throws Exception {
        // given
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID orderId = UUID.fromString("d9a28926-8f22-49bb-8044-a51b83074e50");

        long amount = 250000L;

        LocalDateTime createdAt = LocalDateTime.of(2025, 12, 7, 15, 13, 42);
        LocalDateTime auctionStartTime = LocalDateTime.of(2025, 12, 10, 19, 0);

        OrderSummaryInfo info = new OrderSummaryInfo(
                orderId,
                OrderStatus.PAYMENT,
                amount,
                "Nike Air Jordan 1 Retro High OG",
                "조던 1 레트로 하이 한정판 경매 1차",
                auctionStartTime,
                createdAt
        );

        Pageable pageable = PageRequest.of(0, 20);
        Page<OrderSummaryInfo> page = new PageImpl<>(List.of(info), pageable, 1);

        given(orderFacade.getOrders(any(UUID.class), any(Pageable.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/orders")
                        .param("page", "0")
                        .param("size", "20")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 주문 목록 조회 성공"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.content[0].status").value("PAYMENT"))
                .andExpect(jsonPath("$.data.content[0].amount").value((int) amount));
    }

    @Test
    @DisplayName("주문 목록 조회 실패 - 내부 오류 발생 시 예외 전파")
    void getOrders_fail_whenInternalErrorOccurs() throws Exception {
        // given
        String userId = "11111111-2222-3333-4444-555555555555";

        doThrow(new RuntimeException("internal error"))
                .when(orderFacade)
                .getOrders(any(UUID.class), any(Pageable.class));

        // when & then
        assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/v1/orders")
                                .param("page", "0")
                                .param("size", "20")
                                .param("userId", userId)
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn()
        );
    }
}