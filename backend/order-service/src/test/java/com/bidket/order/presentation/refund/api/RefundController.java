// src/test/java/com/bidket/order/presentation/refund/api/RefundControllerTest.java
package com.bidket.order.presentation.refund.api;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidket.order.application.refund.facade.RefundFacade;
import com.bidket.order.domain.refund.model.Refund;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RefundController.class)
class RefundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RefundFacade refundFacade;

    @Test
    @DisplayName("환불 요청 생성 성공 - 200 OK와 ApiResponse 반환")
    void createRefund_success() throws Exception {
        // given
        UUID paymentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-1234567890ab");

        String requestJson = """
                {
                  "refundAmount": 50000,
                  "refundPointAmount": 10000,
                  "reason": "단순 변심"
                }
                """;

        UUID refundId = UUID.fromString("d9a28926-8f22-49bb-8044-a51b83074e50");

        Refund refund = Mockito.mock(Refund.class);
        Mockito.when(refund.id()).thenReturn(refundId);
        Mockito.when(refund.paymentId()).thenReturn(paymentId);
        Mockito.when(refund.refundAmount()).thenReturn(50_000L);

        given(refundFacade.createRefund(
                any(UUID.class),
                anyLong(),
                anyLong(),
                anyString()
        )).willReturn(refund);

        // when & then
        mockMvc.perform(post("/v1/refunds/{paymentId}", paymentId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("환불 요청이 접수되었습니다.")))
                .andExpect(jsonPath("$.data.refundId").value(refundId.toString()))
                .andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.refundAmount").value(50_000));
    }

    @Test
    @DisplayName("환불 요청 생성 실패 - 내부 오류 발생 시 예외 전파")
    void createRefund_fail_whenInternalErrorOccurs() throws Exception {
        // given
        UUID paymentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-1234567890ab");

        String requestJson = """
                {
                  "refundAmount": 50000,
                  "refundPointAmount": 10000,
                  "reason": "단순 변심"
                }
                """;

        doThrow(new RuntimeException("internal error"))
                .when(refundFacade)
                .createRefund(
                        any(UUID.class),
                        anyLong(),
                        anyLong(),
                        anyString()
                );

        // when & then
        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/v1/refunds/{paymentId}", paymentId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                        .andReturn()
        );
    }
}