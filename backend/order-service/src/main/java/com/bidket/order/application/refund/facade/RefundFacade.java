package com.bidket.order.application.refund.facade;

import com.bidket.order.domain.refund.model.Refund;
import com.bidket.order.domain.refund.repository.RefundRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundFacade {

    private final RefundRepository refundRepository;

    public Refund createRefund(
            UUID paymentId,
            Long refundAmount,
            Long refundPointAmount,
            String reason
    ) {
        Refund refund = Refund.request(
                paymentId,
                refundAmount,
                refundPointAmount,
                reason,
                LocalDateTime.now()
        );

        // TODO: Payple 환불 연동 시 실제 PG 환불 요청 및 상태 업데이트 처리 추가
        return refundRepository.save(refund);
    }
}