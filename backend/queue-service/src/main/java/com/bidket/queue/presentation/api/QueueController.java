package com.bidket.queue.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.queue.application.facade.QueueFacade;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import com.bidket.queue.presentation.dto.response.QueueAccommodatableResponse;
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

    @PostMapping("/internal/queues")
    public Mono<ResponseEntity<ApiResponse<QueueCreateResponse>>> createQueueConfig(@RequestBody @Valid QueueCreateRequest request) {
        return queueFacade.createConfigQueue(request)
                .map(response -> ResponseEntity
                        .created(URI.create("/v1/internal/queues/" + response.auctionId()))
                        .body(ApiResponse.success("queue config 생성", response)));
    }

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

    @GetMapping("/queues/{auctionId}/status")
    public Mono<ResponseEntity<ApiResponse<QueueAccommodatableResponse>>> getQueueStats(@PathVariable UUID auctionId) {
        UUID userId = UUID.fromString("3cd28e63-55fc-47f9-b0a7-f3ccaabb78b9");
        return queueFacade.getQueueStatus(userId, auctionId)
                .map(response ->
                        ResponseEntity
                                .ok()
                                .header("X-ACTIVE_TOKEN", response.token())
                                .body(ApiResponse.success(response))
                );
    }

    @DeleteMapping("/queues/{auctionId}")
    public Mono<ResponseEntity<Void>> cancelWaiting(@PathVariable UUID auctionId) {
        UUID userId = UUID.randomUUID();
        return queueFacade.cancelWaiting(userId, auctionId)
                .then(Mono.fromCallable(() -> ResponseEntity.noContent()
                        .location(URI.create("/temp"))
                        .build()));
    }
}
