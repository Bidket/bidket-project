package com.bidket.queue.domain.model.outbox;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.model.QueueErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class QueueOutboxModel {
    private UUID id;
    private UUID aggregateId;
    private String aggregateType;
    private String topic;
    private String key;
    private String payload;
    private EventType eventType;
    private UUID correlationId;
    private int retryCount;
    private String errorMessage;
    private LocalDateTime publishedAt;
    private OutboxStatus status;
    private boolean isNew;

    public void retry() {
        if(retryCount < 5)
            retryCount++;
        else
            status = OutboxStatus.FAILED;
    }

    public void published() {
        publishedAt = LocalDateTime.now();
        status = OutboxStatus.PUBLISHED;
    }

}
