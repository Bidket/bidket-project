package com.bidket.order.application.refund.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import com.bidket.order.domain.refund.model.Refund;
import com.bidket.order.domain.refund.repository.RefundRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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
        UUID paymentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-1234567890ab");
        long refundAmount = 50_000L;
        long refundPointAmount = 10_000L;
        String reason = "단순 변심";

        Refund savedRefund = Mockito.mock(Refund.class);

        Mockito.when(refundRepository.save(any(Refund.class)))
                .thenReturn(savedRefund);

        // when
        Refund result = refundFacade.createRefund(
                paymentId,
                refundAmount,
                refundPointAmount,
                reason
        );

        // then
        assertThat(result).isEqualTo(savedRefund);
    }

    @Test
    @DisplayName("환불 생성 실패 - Repository에서 예외 발생 시 예외를 전파한다")
    void createRefund_fail_whenRepositoryThrows() {
        // given
        UUID paymentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-1234567890ab");
        long refundAmount = 50_000L;
        long refundPointAmount = 10_000L;
        String reason = "단순 변심";

        Mockito.when(refundRepository.save(any(Refund.class)))
                .thenThrow(new RuntimeException("DB error"));

        // when & then
        assertThrows(RuntimeException.class, () ->
                refundFacade.createRefund(
                        paymentId,
                        refundAmount,
                        refundPointAmount,
                        reason
                )
        );
    }
}