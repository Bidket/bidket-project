package com.bidket.order.presentation.refund.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.common.presentation.response.PageResponse;
import com.bidket.order.application.refund.facade.RefundFacade;
import com.bidket.order.application.refund.info.RefundSummaryInfo;
import com.bidket.order.domain.refund.model.Refund;
import com.bidket.order.presentation.refund.dto.request.RefundCreateRequest;
import com.bidket.order.presentation.refund.dto.response.RefundCreateResponse;
import com.bidket.order.presentation.refund.dto.response.RefundSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Refund", description = "환불 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/refunds")
public class RefundController {

    private final RefundFacade refundFacade;

    @PostMapping("/{paymentId}")
    @Operation(
            summary = "환불 요청",
            description = "결제 ID와 환불 정보를 기반으로 환불 요청을 생성합니다.\n\n"
                    + "※ Payple(PG) 환불 연동 전 단계로, 현재는 내부 환불 요청 데이터만 생성합니다."
    )
    public ApiResponse<RefundCreateResponse> createRefund(
            @RequestParam UUID userId,
            @PathVariable UUID paymentId,
            @RequestBody RefundCreateRequest request
    ) {
        // TODO: 인증 적용 예정

        Refund refund = refundFacade.createRefund(
                userId,
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

    @GetMapping
    @Operation(
            summary = "환불 목록 조회",
            description = "현재 로그인한 회원의 환불 내역을 페이지네이션으로 조회합니다.\n\n"
                    + "※ TODO: 추후 인증 적용 예정 "
    )
    public ApiResponse<PageResponse<RefundSummaryResponse>> getMyRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = true) UUID userId   // TODO: 인증 적용 예정
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<RefundSummaryInfo> refunds = refundFacade.getMyRefunds(userId, pageable);

        List<RefundSummaryResponse> content = refunds.getContent().stream()
                .map(RefundSummaryResponse::from)
                .toList();

        PageResponse<RefundSummaryResponse> response = PageResponse.of(
                content,
                refunds.getNumber(),
                refunds.getSize(),
                refunds.getTotalElements()
        );

        return ApiResponse.success(
                "환불 내역을 조회했습니다.",
                response
        );
    }
}