package com.bidket.queue.presentation.dto.request;

import com.bidket.queue.domain.model.QueueConfigModel;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record QueueCreateRequest(
        @NotNull UUID auctionId,
        @NotNull Long maxActive,
        @NotNull Integer permitsPerSec,
        @NotNull LocalDateTime openAt,
        @NotNull LocalDateTime closeAt
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
