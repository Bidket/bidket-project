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
        @Index(name = "idx_notification_user_read", columnList = "user_id, read_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "user_id", nullable = true, columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // QUEUE_CALL, BID_SUCCESS, PAYMENT_DONE 등 (기존 호환성 유지용)

    @Column(name = "category", nullable = true, length = 50)
    private String category; // AUCTION_START, BID_SUCCESS, PAYMENT_EXPIRE 등

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

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder
    public Notification(UUID userId, String type, String category, NotificationChannel channel,
                       String title, String message, String linkUrl, String payload, NotificationStatus status) {
        this.userId = userId;
        this.type = type;
        this.category = category;
        this.channel = channel != null ? channel : NotificationChannel.PUSH;
        this.title = title;
        this.message = message;
        this.linkUrl = linkUrl;
        this.payload = payload;
        this.status = status != null ? status : NotificationStatus.PENDING;
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

