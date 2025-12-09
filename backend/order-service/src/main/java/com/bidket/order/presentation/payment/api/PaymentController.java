package com.bidket.order.presentation.payment.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.order.application.payment.facade.PaymentFacade;
import com.bidket.order.presentation.payment.dto.request.PaymentCreateRequest;
import com.bidket.order.presentation.payment.dto.response.PaymentCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment", description = "결제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @Operation(
            summary = "결제 요청 생성",
            description = "주문 ID와 결제 정보를 기반으로 결제 요청을 생성합니다.\n\n"
                    + "※ Payple(PG) 연동 전 단계로, 현재는 내부 결제 요청 데이터만 생성합니다."
    )
    @PostMapping
    public ApiResponse<PaymentCreateResponse> createPayment(
            @RequestBody PaymentCreateRequest request
    ) {
        // TODO: 인증 적용 예정

        var payment = paymentFacade.createPayment(
                request.orderId(),
                request.method(),
                request.amount(),
                request.usePointAmount()
        );

        return ApiResponse.success(
                "결제 요청이 생성되었습니다.",
                PaymentCreateResponse.from(payment)
        );
    }
}