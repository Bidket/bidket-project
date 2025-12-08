package com.bidket.queue.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.queue.application.facade.QueueFacade;
import com.bidket.queue.presentation.dto.request.QueueConfigUpdateRequest;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.*;
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

    @DeleteMapping("/queues/{auctionId}")
    public Mono<ResponseEntity<Void>> cancelWaiting(@PathVariable UUID auctionId) {
        UUID userId = UUID.randomUUID();
        return queueFacade.cancelWaiting(userId, auctionId)
                .then(Mono.fromCallable(() -> ResponseEntity.noContent()
                        .location(URI.create("/temp"))
                        .build()));
    }

    @GetMapping("/queues/{auctionId}")
    public Mono<ResponseEntity<ApiResponse<QueueStatusResponse>>> getQueueStatus(@PathVariable UUID auctionId) {
        return queueFacade.getQueueStatus(auctionId)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }

    @PostMapping("/queues/{auctionId}/heartbeat")
    public Mono<ResponseEntity<ApiResponse<QueueHeartbeatResponse>>> heartbeat(@PathVariable UUID auctionId) {
        UUID userId = UUID.fromString("983c3afb-14b4-4a30-b4fe-80168202fc7e");
        String token = "eyJhbGciOiJIUzM4NCJ9.eyJ1c2VySWQiOiI5ODNjM2FmYi0xNGI0LTRhMzAtYjRmZS04MDE2ODIwMmZjN2UiLCJhdWN0aW9uSWQiOiIzZmE4NWY2NC01NzE3LTQ1NjItYjNmYy0yYzk2M2Y2NmFmYTkiLCJpYXQiOjE3NjUxMTU0NDMsImV4cCI6MTc3MjMxNTQ0M30.eOY4IU7Pb-zqv3_TCKZb3WLpXFhFfMPY1Z_Nas8WZpZEvqJzWzuoq_XF-66jsSst";
        return queueFacade.heartbeat(userId, auctionId, token)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }

    @PatchMapping("/admin/queues/{auctionId}")
    public Mono<ResponseEntity<ApiResponse<QueueConfigUpdateResponse>>> updateConfig(@PathVariable UUID auctionId, @RequestBody QueueConfigUpdateRequest request) {
        return queueFacade.updateConfig(auctionId, request)
                .map(response ->
                        ResponseEntity.ok(ApiResponse.success(response))
                );
    }
}
