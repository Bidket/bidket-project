package com.bidket.order.presentation.payment.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.common.presentation.response.PageResponse;
import com.bidket.order.application.payment.facade.PaymentFacade;
import com.bidket.order.application.payment.info.PaymentSummaryInfo;
import com.bidket.order.presentation.payment.dto.request.PaymentCreateRequest;
import com.bidket.order.presentation.payment.dto.response.PaymentCreateResponse;
import com.bidket.order.presentation.payment.dto.response.PaymentSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            @RequestParam String userId,              // TODO: 인증 적용 예정 (토큰에서 userId 추출)
            @RequestBody PaymentCreateRequest request
    ) {
        UUID userUuid = UUID.fromString(userId);

        var payment = paymentFacade.createPayment(
                userUuid,
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

    @GetMapping
    @Operation(
            summary = "결제 목록 조회",
            description = "현재 로그인한 회원의 결제 내역을 페이지네이션으로 조회합니다."
    )
    public ApiResponse<PageResponse<PaymentSummaryResponse>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam String userId            // TODO: 인증 적용 예정 (토큰에서 userId 추출)
    ) {
        UUID userUuid = UUID.fromString(userId);
        Pageable pageable = PageRequest.of(page, size);

        Page<PaymentSummaryInfo> payments = paymentFacade.getMyPayments(userUuid, pageable);

        List<PaymentSummaryResponse> content = payments.getContent().stream()
                .map(PaymentSummaryResponse::from)
                .toList();

        PageResponse<PaymentSummaryResponse> response = PageResponse.of(
                content,
                payments.getNumber(),
                payments.getSize(),
                payments.getTotalElements()
        );

        return ApiResponse.success("결제 내역을 조회했습니다.", response);
    }
}