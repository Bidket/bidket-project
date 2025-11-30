package com.bidket.queue.presentation.api;

import com.bidket.queue.application.service.QueueService;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
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
    public Mono<ResponseEntity<QueueCreateResponse>> createQueueConfig(@RequestBody QueueCreateRequest request) {
        return queueService.createQueue(request)
                .map(response -> ResponseEntity
                        .created(URI.create("/v1/internal/queues/" + response.auctionId()))
                        .body(response));
    }
}
