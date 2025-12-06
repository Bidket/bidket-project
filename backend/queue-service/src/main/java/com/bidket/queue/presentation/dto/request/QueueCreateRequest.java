package com.bidket.queue.presentation.dto.request;

import com.bidket.queue.domain.model.QueueConfigModel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record QueueCreateRequest(
        @NotNull(message = "경매 ID는 null일 수 없습니다.")
        UUID auctionId,
        @NotNull(message = "대기열 최대 수용인원은 null일 수 없습니다.")
        @Min(value = 100, message = "대기열 최대 수용인원은 100 미만 일 수 없습니다.")
        Long maxActive,
        @NotNull
        @Min(value = 1, message = "대기열 최대 수용인원은 1 미만 일 수 없습니다.")
        Integer permitsPerSec,
        @NotNull(message = "경매 시작 시간은 null일 수 없습니다.")
        Instant openAt,
        @NotNull(message = "경매 종료 시간은 null일 수 없습니다.")
        Instant closeAt
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
