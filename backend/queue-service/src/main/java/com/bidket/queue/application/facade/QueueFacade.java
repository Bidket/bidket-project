package com.bidket.queue.application.facade;

import com.bidket.queue.application.service.QueueInternalService;
import com.bidket.queue.application.service.QueueTrafficService;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import com.bidket.queue.presentation.dto.response.QueueStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueFacade {
    private final QueueInternalService queueInternalService;
    private final QueueTrafficService queueTrafficService;

    public Mono<QueueCreateResponse> createConfigQueue(QueueCreateRequest request) {
        return queueInternalService.createConfigQueue(request);
    }

    public Mono<QueueEnterResponse> enterQueue(UUID userId, UUID auctionId) {
        return queueTrafficService.enterQueue(userId, auctionId);
    }

    public Mono<QueueStatusResponse> getQueueStatus(UUID userId, UUID auctionId) {
        return queueTrafficService.getQueueStatus(userId, auctionId);
    }

    public Mono<String> cancelWaiting(UUID userId, UUID auctionId) {
        return queueTrafficService.cancelWaiting(userId, auctionId);
    }
}
