package com.bidket.order.presentation.refund.api;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidket.order.application.refund.facade.RefundFacade;
import com.bidket.order.application.refund.info.RefundSummaryInfo;
import com.bidket.order.domain.refund.model.Refund;
import com.bidket.order.domain.refund.model.RefundStatus;
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

@WebMvcTest(RefundController.class)
class RefundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RefundFacade refundFacade;

    @Test
    @DisplayName("환불 생성 성공 - 200 OK와 ApiResponse 반환")
    void createRefund_success() throws Exception {
        // given
        String requestJson = """
                {
                  "refundAmount": 50000,
                  "refundPointAmount": 10000,
                  "reason": "단순 변심"
                }
                """;

        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID paymentId = UUID.fromString("81dcb0aa-53d9-4724-bca8-57e94a52b929");
        UUID refundId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        long refundAmount = 50_000L;
        long refundedPointAmount = 10_000L;
        String reason = "단순 변심";

        LocalDateTime requestedAt = LocalDateTime.of(2025, 12, 10, 12, 0);

        Refund refund = new Refund(
                refundId,
                userId,
                paymentId,
                refundAmount,
                refundedPointAmount,
                RefundStatus.REQUESTED,
                reason,
                requestedAt,
                null
        );

        org.mockito.BDDMockito.given(
                refundFacade.createRefund(
                        any(UUID.class),
                        any(UUID.class),
                        anyLong(),
                        anyLong(),
                        any(String.class)
                )
        ).willReturn(refund);

        // when & then
        mockMvc.perform(post("/v1/refunds/{paymentId}", paymentId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("환불 요청이 접수되었습니다.")))
                .andExpect(jsonPath("$.data.refundId").value(refundId.toString()))
                .andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.refundAmount").value((int) refundAmount))
                .andExpect(jsonPath("$.data.refundedPointAmount").value((int) refundedPointAmount))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("환불 생성 실패 - 내부 오류 발생 시 예외 전파")
    void createRefund_fail_whenInternalErrorOccurs() throws Exception {
        // given
        String requestJson = """
                {
                  "refundAmount": 50000,
                  "refundPointAmount": 10000,
                  "reason": "단순 변심"
                }
                """;

        UUID paymentId = UUID.fromString("81dcb0aa-53d9-4724-bca8-57e94a52b929");
        String userId = "11111111-2222-3333-4444-555555555555";

        org.mockito.Mockito.doThrow(new RuntimeException("internal error"))
                .when(refundFacade)
                .createRefund(
                        any(UUID.class),
                        any(UUID.class),
                        anyLong(),
                        anyLong(),
                        any(String.class)
                );

        // when & then
        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/v1/refunds/{paymentId}", paymentId)
                                .param("userId", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                        .andReturn()
        );
    }

    @Test
    @DisplayName("환불 목록 조회 성공 - 200 OK와 PageResponse 반환")
    void getMyRefunds_success() throws Exception {
        // given
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID paymentId = UUID.fromString("81dcb0aa-53d9-4724-bca8-57e94a52b929");
        UUID refundId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        long refundAmount = 50_000L;
        long refundedPointAmount = 10_000L;
        LocalDateTime approvedAt = null;

        RefundSummaryInfo info = new RefundSummaryInfo(
                refundId,
                paymentId,
                refundAmount,
                refundedPointAmount,
                RefundStatus.REQUESTED,
                approvedAt
        );

        Pageable pageable = PageRequest.of(0, 20);
        Page<RefundSummaryInfo> page = new PageImpl<>(List.of(info), pageable, 1);

        org.mockito.BDDMockito.given(
                        refundFacade.getMyRefunds(any(UUID.class), any(Pageable.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/refunds")
                        .param("page", "0")
                        .param("size", "20")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("환불 내역을 조회했습니다."))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].refundId").value(refundId.toString()))
                .andExpect(jsonPath("$.data.content[0].paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.content[0].refundAmount").value((int) refundAmount))
                .andExpect(jsonPath("$.data.content[0].refundedPointAmount").value(
                        (int) refundedPointAmount))
                .andExpect(jsonPath("$.data.content[0].status").value("REQUESTED"));
    }

    @Test
    @DisplayName("환불 목록 조회 실패 - 내부 오류 발생 시 예외 전파")
    void getMyRefunds_fail_whenInternalErrorOccurs() throws Exception {
        // given
        String userId = "11111111-2222-3333-4444-555555555555";

        org.mockito.Mockito.doThrow(new RuntimeException("internal error"))
                .when(refundFacade)
                .getMyRefunds(any(UUID.class), any(Pageable.class));

        // when & then
        assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/v1/refunds")
                                .param("page", "0")
                                .param("size", "20")
                                .param("userId", userId)
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn()
        );
    }
}