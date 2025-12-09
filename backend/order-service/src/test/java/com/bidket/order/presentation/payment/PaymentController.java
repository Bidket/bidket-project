package com.bidket.order.presentation.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidket.order.application.payment.facade.PaymentFacade;
import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.model.PaymentMethod;
import com.bidket.order.domain.payment.model.PaymentStatus;
import com.bidket.order.presentation.payment.api.PaymentController;
import com.bidket.order.presentation.payment.dto.request.PaymentCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentFacade paymentFacade;

    @Test
    @DisplayName("결제 요청 생성 성공 - 200 OK와 ApiResponse 래핑 응답을 반환한다")
    void createPayment_success() throws Exception {
        // given
        UUID orderId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-1234567890ab");
        UUID paymentId = UUID.fromString("61ef50cf-0e17-43e3-baf8-f8b65e3eea47");
        Long amount = 50_000L;
        Long usedPointAmount = 10_000L;
        LocalDateTime now = LocalDateTime.of(2025, 12, 7, 22, 2, 39);

        Payment payment = new Payment(
                paymentId,
                orderId,
                PaymentMethod.CARD,
                amount,
                usedPointAmount,
                PaymentStatus.PENDING,
                now,
                now
        );

        Mockito.when(paymentFacade.createPayment(
                        any(UUID.class),
                        anyString(),
                        anyLong(),
                        anyLong()
                ))
                .thenReturn(payment);

        PaymentCreateRequest request = new PaymentCreateRequest(
                orderId,
                "CARD",
                amount,
                usedPointAmount
        );

        String json = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("결제 요청이 생성되었습니다."))
                .andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.method").value("CARD"))
                .andExpect(jsonPath("$.data.amount").value(amount.intValue()))
                .andExpect(jsonPath("$.data.usedPointAmount").value(usedPointAmount.intValue()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("결제 요청 생성 실패 - Facade에서 예외 발생 시 예외가 전파된다")
    void createPayment_fail_whenFacadeThrows() throws Exception {
        // given
        UUID orderId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-1234567890ab");
        Long amount = 50_000L;
        Long usedPointAmount = 10_000L;

        PaymentCreateRequest request = new PaymentCreateRequest(
                orderId,
                "CARD",
                amount,
                usedPointAmount
        );

        String json = objectMapper.writeValueAsString(request);

        Mockito.when(paymentFacade.createPayment(
                        any(UUID.class),
                        anyString(),
                        anyLong(),
                        anyLong()
                ))
                .thenThrow(new RuntimeException("DB error"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> mockMvc.perform(post("/v1/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andReturn()
        );
    }
}