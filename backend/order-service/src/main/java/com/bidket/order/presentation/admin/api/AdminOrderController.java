package com.bidket.order.presentation.admin.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.common.presentation.response.PageResponse;
import com.bidket.order.application.admin.facade.AdminOrderFacade;
import com.bidket.order.application.admin.info.AdminOrderSummaryInfo;
import com.bidket.order.domain.order.model.OrderStatus;
import com.bidket.order.presentation.admin.dto.request.AdminOrderStatusChangeRequest;
import com.bidket.order.presentation.admin.dto.response.AdminOrderDetailResponse;
import com.bidket.order.presentation.admin.dto.response.AdminOrderSummaryResponse;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final AdminOrderFacade adminOrderFacade;

    @GetMapping
    public ApiResponse<PageResponse<AdminOrderSummaryResponse>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status
    ) {
        Page<AdminOrderSummaryInfo> result =
                adminOrderFacade.getOrders(status, PageRequest.of(page, size));

        List<AdminOrderSummaryResponse> content = result.getContent()
                .stream()
                .map(AdminOrderSummaryResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.success(
                "관리자 주문 목록 조회 성공",
                PageResponse.of(content, page, size, result.getTotalElements())
        );
    }

    @GetMapping("/{orderId}")
    public ApiResponse<AdminOrderDetailResponse> getOrder(@PathVariable UUID orderId) {
        return ApiResponse.success(
                "관리자 주문 상세 조회 성공",
                AdminOrderDetailResponse.from(adminOrderFacade.getOrder(orderId))
        );
    }

    @PatchMapping("/{orderId}/status")
    public ApiResponse<AdminOrderDetailResponse> changeStatus(
            @PathVariable UUID orderId,
            @RequestBody AdminOrderStatusChangeRequest request
    ) {
        return ApiResponse.success(
                "주문 상태가 변경되었습니다.",
                AdminOrderDetailResponse.from(
                        adminOrderFacade.changeStatus(
                                orderId,
                                request.status(),
                                request.reason()
                        )
                )
        );
    }
}