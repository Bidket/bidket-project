package com.bidket.queue.application.facade;

import com.bidket.queue.application.service.QueueManagementService;
import com.bidket.queue.application.service.QueueTrafficService;
import com.bidket.queue.presentation.dto.request.QueueConfigUpdateRequest;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueFacade {
    private final QueueManagementService queueManagementService;
    private final QueueTrafficService queueTrafficService;

    public Mono<QueueCreateResponse> createConfigQueue(QueueCreateRequest request) {
        return queueManagementService.createConfigQueue(request);
    }

    public Mono<QueueEnterResponse> enterQueue(UUID userId, UUID auctionId) {
        return queueTrafficService.enterQueue(userId, auctionId);
    }

    public Mono<QueueAccommodatableResponse> isAccommodatable(UUID userId, UUID auctionId) {
        return queueTrafficService.isAccommodatable(userId, auctionId);
    }

    public Mono<Void> cancelWaiting(UUID userId, UUID auctionId) {
        return queueTrafficService.cancelWaiting(userId, auctionId);
    }

    public Mono<QueueStatusResponse> getQueueStatus(UUID auctionId) {
        return queueTrafficService.getQueueStatus(auctionId);
    }

    public Mono<QueueHeartbeatResponse> heartbeat(UUID userId, UUID auctionId, String token) {
        return queueTrafficService.heartbeat(userId, auctionId, token);
    }

    public Mono<QueueConfigUpdateResponse> updateConfig(UUID auctionId, QueueConfigUpdateRequest request) {
        return queueManagementService.updateConfig(auctionId, request);
    }
}
