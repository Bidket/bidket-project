package com.bidket.order.application.refund.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import com.bidket.order.application.refund.info.RefundSummaryInfo;
import com.bidket.order.domain.refund.model.Refund;
import com.bidket.order.domain.refund.model.RefundStatus;
import com.bidket.order.domain.refund.repository.RefundRepository;
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
class RefundFacadeTest {

    @Mock
    private RefundRepository refundRepository;

    @InjectMocks
    private RefundFacade refundFacade;

    @Test
    @DisplayName("환불 생성 성공 - Refund를 반환한다")
    void createRefund_success() {
        // given
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID paymentId = UUID.fromString("81dcb0aa-53d9-4724-bca8-57e94a52b929");
        UUID refundId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        long refundAmount = 50_000L;
        long refundedPointAmount = 10_000L;
        String reason = "단순 변심";

        LocalDateTime now = LocalDateTime.now();

        Refund savedRefund = new Refund(
                refundId,
                userId,
                paymentId,
                refundAmount,
                refundedPointAmount,
                RefundStatus.REQUESTED,
                reason,
                now,
                null
        );

        given(refundRepository.save(org.mockito.ArgumentMatchers.any(Refund.class)))
                .willReturn(savedRefund);

        // when
        Refund result = refundFacade.createRefund(
                userId,
                paymentId,
                refundAmount,
                refundedPointAmount,
                reason
        );

        // then
        assertThat(result.id()).isEqualTo(refundId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.paymentId()).isEqualTo(paymentId);
        assertThat(result.refundAmount()).isEqualTo(refundAmount);
        assertThat(result.refundedPointAmount()).isEqualTo(refundedPointAmount);
        assertThat(result.status()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(result.reason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("환불 생성 실패 - Repository에서 예외 발생 시 예외를 전파한다")
    void createRefund_fail_whenRepositoryThrows() {
        // given
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID paymentId = UUID.fromString("81dcb0aa-53d9-4724-bca8-57e94a52b929");

        long refundAmount = 50_000L;
        long refundedPointAmount = 10_000L;
        String reason = "단순 변심";

        doThrow(new RuntimeException("DB error"))
                .when(refundRepository)
                .save(org.mockito.ArgumentMatchers.any(Refund.class));

        // when & then
        assertThrows(RuntimeException.class, () ->
                refundFacade.createRefund(
                        userId,
                        paymentId,
                        refundAmount,
                        refundedPointAmount,
                        reason
                )
        );
    }

    @Test
    @DisplayName("환불 목록 조회 성공 - 환불 요약 정보를 페이징으로 반환한다")
    void getMyRefunds_success() {
        // given
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID paymentId = UUID.fromString("81dcb0aa-53d9-4724-bca8-57e94a52b929");
        UUID refundId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        long refundAmount = 50_000L;
        long refundedPointAmount = 10_000L;

        LocalDateTime requestedAt = LocalDateTime.of(2025, 12, 10, 12, 0);
        LocalDateTime approvedAt = null;

        Refund refund = new Refund(
                refundId,
                userId,
                paymentId,
                refundAmount,
                refundedPointAmount,
                RefundStatus.REQUESTED,
                "단순 변심",
                requestedAt,
                approvedAt
        );

        Pageable pageable = PageRequest.of(0, 20);
        Page<Refund> refundPage = new PageImpl<>(List.of(refund), pageable, 1);

        given(refundRepository.findByUserId(userId, pageable))
                .willReturn(refundPage);

        // when
        Page<RefundSummaryInfo> result = refundFacade.getMyRefunds(userId, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);

        RefundSummaryInfo info = result.getContent().get(0);
        assertThat(info.refundId()).isEqualTo(refundId);
        assertThat(info.paymentId()).isEqualTo(paymentId);
        assertThat(info.refundAmount()).isEqualTo(refundAmount);
        assertThat(info.refundedPointAmount()).isEqualTo(refundedPointAmount);
        assertThat(info.status()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(info.approvedAt()).isNull();
    }

    @Test
    @DisplayName("환불 목록 조회 실패 - Repository에서 예외 발생 시 예외를 전파한다")
    void getMyRefunds_fail_whenRepositoryThrows() {
        // given
        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        Pageable pageable = PageRequest.of(0, 20);

        doThrow(new RuntimeException("DB error"))
                .when(refundRepository)
                .findByUserId(userId, pageable);

        // when & then
        assertThrows(RuntimeException.class, () ->
                refundFacade.getMyRefunds(userId, pageable)
        );
    }
}