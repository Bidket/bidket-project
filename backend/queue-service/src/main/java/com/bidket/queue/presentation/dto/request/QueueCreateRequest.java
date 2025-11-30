package com.bidket.queue.presentation.dto.request;

import com.bidket.queue.domain.model.QueueConfigModel;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record QueueCreateRequest(
        UUID auctionId,
        Long maxActive,
        Integer permitsPerSec,
        LocalDateTime openAt,
        LocalDateTime closeAt
) {
    public QueueConfigModel toModel() {
        return QueueConfigModel.builder()
                .auctionId(auctionId)
                .maxActive(maxActive)
                .permitsPerSec(permitsPerSec)
                .openAt(openAt)
                .closeAt(closeAt)
                .build();
    }
}
