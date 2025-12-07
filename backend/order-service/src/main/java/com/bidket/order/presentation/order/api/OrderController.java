package com.bidket.order.presentation.order.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.order.application.order.facade.OrderFacade;
import com.bidket.order.application.order.info.OrderInfo;
import com.bidket.order.presentation.order.dto.request.OrderCreateRequest;
import com.bidket.order.presentation.order.dto.response.OrderCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
        UUID userId = UUID.fromString(request.userId());

        OrderInfo orderInfo = orderFacade.createOrder(
                userId,
                UUID.fromString(request.auctionId()),
                UUID.fromString(request.shoeId()),
                request.amount(),
                request.usePointAmount()
        );

        OrderCreateResponse response = OrderCreateResponse.from(orderInfo);
        return ApiResponse.success("신발 경매 주문이 생성되었습니다.", response);
    }
}