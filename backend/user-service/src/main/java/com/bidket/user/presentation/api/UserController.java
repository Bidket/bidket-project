package com.bidket.user.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.user.application.service.UserEmailService;
import com.bidket.user.presentation.dto.response.UserEmailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 사용자 관련 API 컨트롤러
 * 내부 서비스 간 통신용 API
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserEmailService userEmailService;

    /**
     * 사용자 이메일 조회 API
     * @param userId 조회할 사용자 ID
     * @return 사용자 이메일 응답 (userId, email)
     */
    @Operation(
            summary = "사용자 이메일 조회",
            description = "사용자 ID로 이메일 주소를 조회합니다. 내부 서비스 간 통신용 API입니다.",
            tags = {"99. 내부 API"},
            operationId = "internal-01-get-user-email"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserEmailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "사용자를 찾을 수 없음"
            )
    })
    @GetMapping("/{userId}/email")
    public ResponseEntity<ApiResponse<UserEmailResponse>> getUserEmail(
            @Parameter(
                    description = "조회할 사용자 ID",
                    example = "7c4d3a1b-2f9d-4c62-9b4a-1d2f34e5a678",
                    required = true
            )
            @PathVariable UUID userId) {
        UserEmailResponse response = userEmailService.getUserEmail(userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("사용자 이메일 조회에 성공했습니다.", response));
    }
}

