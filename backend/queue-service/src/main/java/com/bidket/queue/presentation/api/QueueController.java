package com.bidket.queue.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.queue.application.service.QueueService;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class QueueController {
    private final QueueService queueService;

    @PostMapping("/internal/queues")
    public Mono<ResponseEntity<ApiResponse<QueueCreateResponse>>> createQueueConfig(@RequestBody @Valid QueueCreateRequest request) {
        return queueService.createConfigQueue(request)
                .map(response -> ResponseEntity
                        .created(URI.create("/v1/internal/queues/" + response.auctionId()))
                        .body(ApiResponse.success("queue config 생성", response)));
    }

    @PostMapping("/queues/{auctionId}")
    public Mono<ResponseEntity<ApiResponse<QueueEnterResponse>>> enterQueue(@PathVariable UUID auctionId) {
        // TODO userId 전달 방식 확립 이후 변경
        // UUID userId = UUID.fromString(Objects.requireNonNull(request.headers().firstHeader("USER-ID")));
        UUID userId = UUID.randomUUID();
        return queueService.enterQueue(userId, auctionId)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }
}
