// com/bidket/order/presentation/refund/api/RefundController.java
package com.bidket.order.presentation.refund.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.order.application.refund.facade.RefundFacade;
import com.bidket.order.domain.refund.model.Refund;
import com.bidket.order.presentation.refund.dto.request.RefundCreateRequest;
import com.bidket.order.presentation.refund.dto.response.RefundCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Refund", description = "환불 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/refunds")
public class RefundController {

    private final RefundFacade refundFacade;

    @Operation(
            summary = "환불 요청",
            description = "결제 ID와 환불 정보를 기반으로 환불 요청을 생성합니다.\n\n"
                    + "※ Payple(PG) 환불 연동 전 단계로, 현재는 내부 환불 요청 데이터만 생성합니다."
    )
    @PostMapping("/{paymentId}")
    public ApiResponse<RefundCreateResponse> createRefund(
            @PathVariable UUID paymentId,
            @RequestBody RefundCreateRequest request
    ) {
        // TODO: 인증 적용 예정
        Refund refund = refundFacade.createRefund(
                paymentId,
                request.refundAmount(),
                request.refundPointAmount(),
                request.reason()
        );

        return ApiResponse.success(
                "환불 요청이 접수되었습니다.",
                RefundCreateResponse.from(refund)
        );
    }
}