package com.bidket.product.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.common.presentation.response.PageResponse;
import com.bidket.product.application.service.AdminQueryService;
import com.bidket.product.presentation.dto.request.PageRequestDto;
import com.bidket.product.presentation.dto.response.size.SizeGetAdminResponse;
import com.bidket.product.presentation.dto.response.size.SizeTypeGetAdminResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Product - 어드민 조회", description = "어드민 상품 조회 API")
public class AdminQueryController {

    private final AdminQueryService adminQueryService;

    @Operation(
            summary = "어드민 사이즈 타입 목록 조회",
            description = "어드민이 사이즈 타입 목록을 조회합니다. 특정 상품 타입 ID로 필터링할 수 있습니다."
    )
    @GetMapping("/size-types")
    public ApiResponse<PageResponse<SizeTypeGetAdminResponse>> getSizeTypes(
            //@RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) UUID productTypeId,
            PageRequestDto pageRequest
    ) {
        //adminRoleValidator.validator(role);
        return ApiResponse.success(adminQueryService.getSizeTypes(productTypeId, pageRequest));
    }

    @Operation(
            summary = "어드민 사이즈 목록 조회",
            description = "어드민이 사이즈 목록을 조회합니다. 특정 사이즈 타입 ID로 필터링할 수 있습니다."
    )
    @GetMapping("/sizes")
    public ApiResponse<PageResponse<SizeGetAdminResponse>> getSizes(
            //@RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) UUID sizeTypeId,
            PageRequestDto pageRequest
    ) {
        //adminRoleValidator.validator(role);
        return ApiResponse.success(adminQueryService.getSizes(sizeTypeId, pageRequest));
    }

}
