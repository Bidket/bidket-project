package com.bidket.queue.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.queue.application.facade.QueueFacade;
import com.bidket.queue.presentation.dto.request.QueueConfigUpdateRequest;
import com.bidket.queue.presentation.dto.response.QueueConfigUpdateResponse;
import com.bidket.queue.presentation.dto.response.QueueMetricsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/internal/queues")
@RequiredArgsConstructor
public class QueueAdminController {
    private final QueueFacade queueFacade;

    private static final String X_MEMBER_ID_HEADER = "X-Member-Id";

    @Operation(summary = "대기열 정책 변경", description = "관리자가 대기열의 최대 수용량, 초당 수용량 등의 설정을 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기열 정책 변경 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = QueueConfigUpdateResponse.class))
                    })
            )
    })
    @PatchMapping("/{auctionId}")
    public Mono<ResponseEntity<ApiResponse<QueueConfigUpdateResponse>>> updateConfig(@PathVariable UUID auctionId,
                                                                                     @RequestBody QueueConfigUpdateRequest request,
                                                                                     @RequestHeader(name = X_MEMBER_ID_HEADER) UUID userId) {
        return queueFacade.updateConfig(userId, auctionId, request)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }

    @Operation(summary = "대기열 현황 상세 모니터링", description = "관리자가 대기열의 상태를 지속적으로 모니터링합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기열 현상 상세 조회 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = QueueMetricsResponse.class))
                    })
            )
    })
    @GetMapping("/{auctionId}/metrics")
    public Mono<ResponseEntity<ApiResponse<QueueMetricsResponse>>> getMetrics(@PathVariable UUID auctionId) {
        return queueFacade.getMetrics(auctionId)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }
}
