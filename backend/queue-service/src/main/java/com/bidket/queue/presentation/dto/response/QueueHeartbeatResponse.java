package com.bidket.queue.presentation.dto.response;

import com.bidket.queue.domain.model.HeartbeatStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record QueueHeartbeatResponse(
        UUID userId,
        String activeToken,
        HeartbeatStatus status,
        LocalDateTime enterTime
) {
}
