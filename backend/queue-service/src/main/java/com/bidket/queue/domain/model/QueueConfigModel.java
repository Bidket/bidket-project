package com.bidket.queue.domain.model;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.presentation.dto.request.QueueConfigUpdateRequest;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueConfigUpdateResponse;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record QueueConfigModel(
        UUID auctionId,
        Long maxActive,
        Integer permitsPerSec,
        QueueConfigStatus status,
        Instant openAt,
        Instant closeAt,
        LocalDateTime lastUpdatedAt,
        UUID lastUpdatedBy) {

    public Map<String, String> toMap() {
        return Map.of("auctionId", auctionId.toString(),
                "maxActive", maxActive.toString(),
                "permitsPerSec", permitsPerSec.toString(),
                "status", status.toString(),
                "openAt", openAt.toString(),
                "closeAt", closeAt.toString());
    }

    public QueueCreateResponse toCreateResponse() {
        return QueueCreateResponse.builder()
                .auctionId(auctionId)
                .maxActive(maxActive)
                .permitsPerSec(permitsPerSec)
                .openAt(openAt)
                .closeAt(closeAt)
                .build();
    }

    public QueueConfigUpdateResponse toUpdateResponse() {
        return QueueConfigUpdateResponse.builder()
                .auctionId(auctionId)
                .maxActive(maxActive)
                .permitsPerSec(permitsPerSec)
                .openAt(openAt)
                .closeAt(closeAt)
                .build();
    }

    public void checkOpenStatus(Instant now) {
        if (now.isBefore(openAt))
            throw new QueueException(QueueErrorCode.AUCTION_NOT_OPENED);
        if (now.isAfter(closeAt))
            throw new QueueException(QueueErrorCode.AUCTION_CLOSED);
    }

    public static QueueConfigModel from(QueueCreateRequest request) {
        return QueueConfigModel.builder()
                .auctionId(request.auctionId())
                .maxActive(request.maxActive())
                .permitsPerSec(request.permitsPerSec())
                .status(QueueConfigStatus.RUNNING)
                .openAt(request.openAt())
                .closeAt(request.closeAt())
                .build();
    }
}
