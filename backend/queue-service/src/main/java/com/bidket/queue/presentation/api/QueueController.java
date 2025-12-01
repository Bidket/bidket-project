package com.bidket.queue.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.queue.application.service.QueueService;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class QueueController {
    private final QueueService queueService;

    @PostMapping("/internal/queues")
    public Mono<ResponseEntity<ApiResponse<QueueCreateResponse>>> createQueueConfig(@RequestBody @Valid QueueCreateRequest request) {
        return queueService.createQueue(request)
                .map(response -> ResponseEntity
                        .created(URI.create("/v1/internal/queues/" + response.auctionId()))
                        .body(ApiResponse.success("queue config 생성", response)));
    }
}
