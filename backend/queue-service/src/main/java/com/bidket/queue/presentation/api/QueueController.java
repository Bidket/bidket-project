package com.bidket.queue.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.queue.application.facade.QueueFacade;
import com.bidket.queue.presentation.dto.request.QueueConfigUpdateRequest;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class QueueController {
    private final QueueFacade queueFacade;

    @Operation(summary = "대기열 설정 생성", description = "대기열의 수용 한계치, 초당 수용량 등의 설정 정보를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기열 설정 생성 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = QueueCreateResponse.class))
                    })
            )
    })
    @PostMapping("/internal/queues")
    public Mono<ResponseEntity<ApiResponse<QueueCreateResponse>>> createQueueConfig(@RequestBody @Valid QueueCreateRequest request) {
        return queueFacade.createConfigQueue(request)
                .map(response -> ResponseEntity
                        .created(URI.create("/v1/internal/queues/" + response.auctionId()))
                        .body(ApiResponse.success("queue config 생성", response)));
    }

    @Operation(summary = "대기열 입장", description = "사용자가 대기열에 입장합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기열 입장 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = QueueEnterResponse.class))
                    })
            )
    })
    @PostMapping("/queues/{auctionId}")
    public Mono<ResponseEntity<ApiResponse<QueueEnterResponse>>> enterQueue(@PathVariable UUID auctionId) {
        // TODO userId 전달 방식 확립 이후 변경
        // UUID userId = UUID.fromString(Objects.requireNonNull(request.headers().firstHeader("USER-ID")));
        UUID userId = UUID.randomUUID();
        return queueFacade.enterQueue(userId, auctionId)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }

    @Operation(summary = "대기열 수용 가능 여부확인", description = "대기중인 사용자가 대기열의 수용 가능 여부를 확인합니다. \n이 API는 Polling 방식으로 동작합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기열 수용 가능 여부 확인 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = QueueAccommodatableResponse.class))
                    })
            )
    })
    @GetMapping("/queues/{auctionId}/status")
    public Mono<ResponseEntity<ApiResponse<QueueAccommodatableResponse>>> isAccommodatable(@PathVariable UUID auctionId) {
        UUID userId = UUID.fromString("3cd28e63-55fc-47f9-b0a7-f3ccaabb78b9");
        return queueFacade.isAccommodatable(userId, auctionId)
                .map(response ->
                        ResponseEntity
                                .ok()
                                .header("X-ACTIVE_TOKEN", response.token())
                                .body(ApiResponse.success(response))
                );
    }

    @Operation(summary = "대기 취소", description = "사용자가 대기를 취소합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기열 취소 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK"))
                    })
            )
    })
    @DeleteMapping("/queues/{auctionId}")
    public Mono<ResponseEntity<Void>> cancelWaiting(@PathVariable UUID auctionId) {
        UUID userId = UUID.randomUUID();
        return queueFacade.cancelWaiting(userId, auctionId)
                .then(Mono.fromCallable(() -> ResponseEntity.noContent()
                        .location(URI.create("/temp"))
                        .build()));
    }

    @Operation(summary = "대기열 상태 확인", description = "사용자가 경매 대기열 상태를 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기열 설정 생성 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = QueueStatusResponse.class))
                    })
            )
    })
    @GetMapping("/queues/{auctionId}")
    public Mono<ResponseEntity<ApiResponse<QueueStatusResponse>>> getQueueStatus(@PathVariable UUID auctionId) {
        return queueFacade.getQueueStatus(auctionId)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }

    @Operation(summary = "입장한 사용자 활동 상태 확인", description = "경매에 입장한 사용자의 활동 상태를 지속적으로 확인하여 토큰 만료 기간을 연장합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기열 설정 생성 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = QueueHeartbeatResponse.class))
                    })
            )
    })
    @PostMapping("/queues/{auctionId}/heartbeat")
    public Mono<ResponseEntity<ApiResponse<QueueHeartbeatResponse>>> heartbeat(@PathVariable UUID auctionId) {
        UUID userId = UUID.fromString("983c3afb-14b4-4a30-b4fe-80168202fc7e");
        String token = "eyJhbGciOiJIUzM4NCJ9.eyJ1c2VySWQiOiI5ODNjM2FmYi0xNGI0LTRhMzAtYjRmZS04MDE2ODIwMmZjN2UiLCJhdWN0aW9uSWQiOiIzZmE4NWY2NC01NzE3LTQ1NjItYjNmYy0yYzk2M2Y2NmFmYTkiLCJpYXQiOjE3NjUxMTU0NDMsImV4cCI6MTc3MjMxNTQ0M30.eOY4IU7Pb-zqv3_TCKZb3WLpXFhFfMPY1Z_Nas8WZpZEvqJzWzuoq_XF-66jsSst";
        return queueFacade.heartbeat(userId, auctionId, token)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }

    @Operation(summary = "대기열 정책 변경", description = "관리자가 대기열의 최대 수용량, 초당 수용량 등의 설정을 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기열 설정 생성 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = QueueConfigUpdateResponse.class))
                    })
            )
    })
    @PatchMapping("/admin/queues/{auctionId}")
    public Mono<ResponseEntity<ApiResponse<QueueConfigUpdateResponse>>> updateConfig(@PathVariable UUID auctionId, @RequestBody QueueConfigUpdateRequest request) {
        return queueFacade.updateConfig(auctionId, request)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }

    @Operation(summary = "대기열 현황 상세 모니터링", description = "관리자가 대기열의 상태를 지속적으로 모니터링합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기열 설정 생성 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = QueueMetricsResponse.class))
                    })
            )
    })
    @GetMapping("/admin/queues/{auctionId}/metrics")
    public Mono<ResponseEntity<ApiResponse<QueueMetricsResponse>>> getMetrics(@PathVariable UUID auctionId) {
        return queueFacade.getMetrics(auctionId)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }
}
