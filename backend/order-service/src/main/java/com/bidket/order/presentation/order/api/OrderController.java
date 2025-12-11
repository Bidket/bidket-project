package com.bidket.order.presentation.order.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.common.presentation.response.PageResponse;
import com.bidket.order.application.order.facade.OrderFacade;
import com.bidket.order.application.order.info.OrderInfo;
import com.bidket.order.application.order.info.OrderSummaryInfo;
import com.bidket.order.presentation.order.dto.request.OrderCreateRequest;
import com.bidket.order.presentation.order.dto.response.OrderCreateResponse;
import com.bidket.order.presentation.order.dto.response.OrderSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orders")
@Tag(name = "Order", description = "신발 경매 주문 API")
public class OrderController {

    private final OrderFacade orderFacade;

    @Operation(
            summary = "신발 경매 주문 생성",
            description = "낙찰된 신발 경매에 대해 주문을 생성하고 결제 대기 상태로 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "주문 생성 성공",
                    content = @Content(schema = @Schema(implementation = OrderCreateResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 요청"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "유효하지 않은 경매 상태 또는 이미 처리된 주문"
            )
    })
    @PostMapping
    public ApiResponse<OrderCreateResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request
    ) {
        // TODO: 인증 적용 예정
        OrderInfo orderInfo = orderFacade.createOrder(
                request.userId(),
                request.auctionId(),
                request.shoeId(),
                request.amount(),
                request.usePointAmount()
        );

        OrderCreateResponse response = OrderCreateResponse.from(orderInfo);
        return ApiResponse.success("신발 경매 주문이 생성되었습니다.", response);
    }

    @GetMapping
    @Operation(summary = "주문 목록 조회", description = "현재 로그인한 사용자의 주문 목록을 페이징 형태로 조회합니다.")
    public ApiResponse<PageResponse<OrderSummaryResponse>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam UUID userId // TODO: 인증 적용 예정 (토큰에서 userId 추출)
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<OrderSummaryInfo> orderPage = orderFacade.getOrders(userId, pageable);

        List<OrderSummaryResponse> content = orderPage.getContent()
                .stream()
                .map(info -> new OrderSummaryResponse(
                        info.getOrderId().toString(),
                        info.getStatus().name(),
                        info.getAmount(),
                        info.getProductName(),
                        info.getAuctionTitle(),
                        info.getAuctionStartTime(),
                        info.getCreatedAt()
                ))
                .collect(Collectors.toList());

        PageResponse<OrderSummaryResponse> response = PageResponse.of(
                content,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements()
        );

        return ApiResponse.success("내 주문 목록 조회 성공", response);
    }
}