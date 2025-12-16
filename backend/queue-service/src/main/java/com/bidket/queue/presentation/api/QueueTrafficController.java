package com.bidket.queue.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.queue.application.facade.QueueFacade;
import com.bidket.queue.presentation.dto.response.QueueAccommodatableResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import com.bidket.queue.presentation.dto.response.QueueHeartbeatResponse;
import com.bidket.queue.presentation.dto.response.QueueStatusResponse;
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

import java.net.URI;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/queues")
@RequiredArgsConstructor
public class QueueTrafficController {
    private final QueueFacade queueFacade;

    private static final String X_USER_ID = "X-User-Id";
    private static final String X_ACTIVE_TOKEN_HEADER = "X-ACTIVE-TOKEN";

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
    @PostMapping("/{auctionId}")
    public Mono<ResponseEntity<ApiResponse<QueueEnterResponse>>> enterQueue(@PathVariable UUID auctionId,
                                                                            @RequestHeader(name = X_USER_ID) UUID userId) {
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
    @GetMapping("/{auctionId}/status")
    public Mono<ResponseEntity<ApiResponse<QueueAccommodatableResponse>>> isAccommodatable(@PathVariable UUID auctionId,
                                                                                           @RequestHeader(name = X_USER_ID) UUID userId) {
        log.info("X-USER-ID: {}", userId);
        return queueFacade.isAccommodatable(userId, auctionId)
                .map(response -> {
                    if (response.token() != null)
                        return ResponseEntity.ok()
                                .header("X-ACTIVE-TOKEN", response.token())
                                .location(URI.create("/v1/auctions/" + auctionId))
                                .body(ApiResponse.success(response));

                    return ResponseEntity
                            .ok()
                            .body(ApiResponse.success(response));
                });
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
    @DeleteMapping("/{auctionId}")
    public Mono<ResponseEntity<Void>> cancelWaiting(@PathVariable UUID auctionId,
                                                    @RequestHeader(name = X_USER_ID) UUID userId) {
        return queueFacade.cancelWaiting(userId, auctionId)
                .then(Mono.fromCallable(() -> ResponseEntity.noContent()
                        .location(URI.create("/temp"))
                        .build()));
    }

    @Operation(summary = "대기열 상태 확인", description = "사용자가 경매 대기열 상태를 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기열 상태 조회 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = QueueStatusResponse.class))
                    })
            )
    })
    @GetMapping("/{auctionId}")
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
                    description = "사용자 활동 상태 확인 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = QueueHeartbeatResponse.class))
                    })
            )
    })
    @PostMapping("/{auctionId}/heartbeat")
    public Mono<ResponseEntity<ApiResponse<QueueHeartbeatResponse>>> heartbeat(@PathVariable UUID auctionId,
                                                                               @RequestHeader(name = X_USER_ID) UUID userId,
                                                                               @RequestHeader(name = X_ACTIVE_TOKEN_HEADER) String token) {
        return queueFacade.heartbeat(userId, auctionId, token)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }
}
