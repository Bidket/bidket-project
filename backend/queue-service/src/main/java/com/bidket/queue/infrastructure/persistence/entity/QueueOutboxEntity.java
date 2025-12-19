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
public class QueueOutboxEntity implements Persistable<String> {
    @Id
    @Column("id")
    private String id;
    @Column("aggregate_id")
    private UUID aggregateId;
    @Column("aggregate_type")
    private String aggregateType;
    @Column("topic")
    private String topic;
    @Column("key")
    private String key;
    @Column("payload")
    private String payload;
    @Column("event_type")
    private EventType eventType;
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

    public QueueOutboxEntity from(QueueOutboxModel model) {
        return QueueOutboxEntity.builder()
                .id(model.id())
                .aggregateId(model.aggregateId())
                .aggregateType(model.aggregateType())
                .topic(model.topic())
                .payload(model.payload())
                .eventType(model.eventType())
                .correlationId(model.correlationId())
                .retryCount(model.retryCount())
                .errorMessage(model.errorMessage())
                .publishedAt(model.publishedAt())
                .status(model.status())
                .build();
    }

    public QueueOutboxModel toModel() {
        return QueueOutboxModel.builder()
                .id(id)
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .topic(topic)
                .payload(payload)
                .eventType(eventType)
                .correlationId(correlationId)
                .retryCount(retryCount)
                .errorMessage(errorMessage)
                .publishedAt(publishedAt)
                .status(status)
                .build();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
