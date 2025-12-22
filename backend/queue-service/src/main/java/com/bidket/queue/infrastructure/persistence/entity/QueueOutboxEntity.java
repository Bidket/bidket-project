package com.bidket.queue.infrastructure.persistence.entity;

import com.bidket.queue.domain.model.outbox.EventType;
import com.bidket.queue.domain.model.outbox.OutboxStatus;
import com.bidket.queue.domain.model.outbox.QueueOutboxModel;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Table("p_queue_outbox")
public class QueueOutboxEntity implements Persistable<UUID> {
    @Id
    @Column("id")
    private UUID id;
    @Column("aggregate_id")
    private UUID aggregateId;
    @Column("aggregate_type")
    private String aggregateType;
    @Column("topic")
    private String topic;
    @Column("partition_key")
    private String key;
    @Column("payload")
    private String payload;
    @Column("event_type")
    private String eventType;
    @Column("correlation_id")
    private UUID correlationId;
    @Column("retry_count")
    private int retryCount;
    @Column("error_message")
    private String errorMessage;
    @Column("published_at")
    private LocalDateTime publishedAt;
    @Column("outbox_status")
    private OutboxStatus status;

    @Transient
    private boolean isNew = true;

    public static QueueOutboxEntity from(QueueOutboxModel model) {
        return QueueOutboxEntity.builder()
                .id(model.getId())
                .aggregateId(model.getAggregateId())
                .aggregateType(model.getAggregateType())
                .topic(model.getTopic())
                .payload(model.getPayload())
                .eventType(model.getEventType().name())
                .correlationId(model.getCorrelationId())
                .retryCount(model.getRetryCount())
                .errorMessage(model.getErrorMessage())
                .publishedAt(model.getPublishedAt())
                .status(model.getStatus())
                .isNew(model.isNew())
                .build();
    }

    public QueueOutboxModel toModel() {
        return QueueOutboxModel.builder()
                .id(id)
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .topic(topic)
                .payload(payload)
                .eventType(EventType.valueOf(eventType))
                .correlationId(correlationId)
                .retryCount(retryCount)
                .errorMessage(errorMessage)
                .publishedAt(publishedAt)
                .status(status)
                .isNew(isNew)
                .build();
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
