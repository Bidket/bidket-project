package com.bidket.queue.presentation.dto.request;

import com.bidket.queue.domain.model.QueueConfigStatus;
import lombok.Builder;

@Builder
public record QueueConfigUpdateRequest(
        Long maxActive,
        Integer permitsPerSec,
        QueueConfigStatus status
) {
}
