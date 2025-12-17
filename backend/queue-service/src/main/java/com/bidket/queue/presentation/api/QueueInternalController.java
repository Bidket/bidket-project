package com.bidket.queue.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.queue.application.facade.QueueFacade;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCloseResponse;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/internal/queues")
@RequiredArgsConstructor
public class QueueInternalController {

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
    @PostMapping
    public Mono<ResponseEntity<ApiResponse<QueueCreateResponse>>> createQueueConfig(@RequestBody @Valid QueueCreateRequest request) {
        return queueFacade.createConfigQueue(request)
                .map(response -> ResponseEntity
                        .created(URI.create("/v1/internal/queues/" + response.auctionId()))
                        .body(ApiResponse.success("queue config 생성", response)));
    }

    @Operation(summary = "대기열 폐쇄", description = "경매 종료시 대기열 폐쇄")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기열 폐쇄 성공",
                    content = @Content(schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(example = "true")),
                            @SchemaProperty(name = "message", schema = @Schema(example = "OK")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = QueueCloseResponse.class))
                    })
            )
    })
    @DeleteMapping("/{auctionId}")
    public Mono<ResponseEntity<ApiResponse<QueueCloseResponse>>> closeQueue(@PathVariable UUID auctionId) {
        return queueFacade.closeQueue(auctionId)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }
}
