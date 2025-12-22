package com.bidket.notification.infrastructure.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import com.bidket.notification.domain.model.NotificationChannel;
import com.bidket.notification.domain.model.NotificationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 알림 엔티티
 */
@Entity
@Table(name = "p_notification", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "user_id"),
        @Index(name = "idx_notification_user_read", columnList = "user_id, read_at"),
        @Index(name = "idx_notification_event_id_channel", columnList = "event_id, channel", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "event_id", nullable = true, columnDefinition = "UUID")
    private UUID eventId; // Kafka 이벤트 ID (멱등성 처리용, UNIQUE(event_id, channel))

    @Column(name = "type", nullable = false, length = 50)
    private String type; // Kafka eventType (near_turn, admitted, outbid, closed, payment_required, paid)

    @Column(name = "category", nullable = true, length = 50)
    private String category; // Kafka topic 기반 카테고리 (QUEUE, AUCTION, ORDER)

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // JSON 문자열로 저장

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "sent_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime sentAt;

    @Column(name = "read_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime readAt;

    // Kafka 이벤트 관련 필드
    @Column(name = "occurred_at", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime occurredAt; // 이벤트 발생 시각 (지연/재처리 분석용)

    @Column(name = "source", nullable = false, length = 50)
    private String source; // 이벤트 발생 서비스 (예: queue-service, order-service)

    @Column(name = "fail_reason", length = 50)
    private String failReason; // 실패 사유 코드 (예: JSON_PARSE_ERROR, VALIDATION_ERROR, DB_TIMEOUT, SLACK_5XX)

    @Column(name = "fail_detail", columnDefinition = "TEXT")
    private String failDetail; // 실패 상세 메시지 (짧게/마스킹 규칙 적용 권장)

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0; // 재시도 누적 횟수 (최대 3회 등 정책 추적용)

    @Column(name = "last_retry_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastRetryAt; // 마지막 재시도 수행 시각

    @Column(name = "trace_id", length = 100)
    private String traceId; // 분산 추적(Zipkin) 연결용 (선택)

    @Builder
    public Notification(UUID userId, UUID eventId, String type, String category, NotificationChannel channel,
                       String title, String message, String linkUrl, String payload, NotificationStatus status,
                       LocalDateTime occurredAt, String source, String failReason, String failDetail,
                       Integer retryCount, LocalDateTime lastRetryAt, String traceId) {
        this.userId = userId;
        this.eventId = eventId;
        this.type = type;
        this.category = category;
        this.channel = channel != null ? channel : NotificationChannel.PUSH;
        this.title = title;
        this.message = message;
        this.linkUrl = linkUrl;
        this.payload = payload;
        this.status = status != null ? status : NotificationStatus.PENDING;
        this.occurredAt = occurredAt;
        this.source = source;
        this.failReason = failReason;
        this.failDetail = failDetail;
        this.retryCount = retryCount != null ? retryCount : 0;
        this.lastRetryAt = lastRetryAt;
        this.traceId = traceId;
    }

    /**
     * 알림 발송 완료 처리
     */
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 알림 발송 실패 처리
     */
    public void markAsFailed() {
        this.status = NotificationStatus.FAILED;
    }

    /**
     * 알림 발송 실패 처리 (상세 정보 포함)
     */
    public void markAsFailed(String failReason, String failDetail) {
        this.status = NotificationStatus.FAILED;
        this.failReason = failReason;
        this.failDetail = failDetail;
    }

    /**
     * 재시도 처리
     */
    public void incrementRetry() {
        this.retryCount = (this.retryCount != null ? this.retryCount : 0) + 1;
        this.lastRetryAt = LocalDateTime.now();
    }

    /**
     * 알림 스킵 처리
     */
    public void markAsSkipped() {
        this.status = NotificationStatus.SKIPPED;
    }

    /**
     * 알림 읽음 처리
     */
    public void markAsRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    /**
     * 읽음 여부 확인
     */
    public boolean isRead() {
        return this.readAt != null;
    }
}

