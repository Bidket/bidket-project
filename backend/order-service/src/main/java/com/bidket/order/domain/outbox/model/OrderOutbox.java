package com.bidket.order.domain.outbox.model;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Order Service OutBox 도메인 모델
 *
 * Transactional Outbox Pattern 구현
 * - 이벤트를 OutBox 테이블에 저장 (트랜잭션 안전성 보장)
 * - 별도 Publisher가 주기적으로 폴링하여 Kafka로 발행
 *
 * 재시도 정책:
 * - PENDING: 즉시 발행 가능
 * - FAILED: 지수 백오프 (0분, 1분, 4분, 16분)
 * - 최대 재시도 횟수 초과 시 발행 불가
 */
public record OrderOutbox(
        UUID id,
        String aggregateType,    // "ORDER", "PAYMENT", "REFUND"
        UUID aggregateId,        // 주문/결제/환불 ID
        String eventType,        // "ORDER_CREATED", "PAYMENT_COMPLETED", etc.
        UUID correlationId,      // Saga 추적 ID
        String payload,          // JSON 직렬화된 이벤트 데이터
        OutboxStatus status,     // PENDING, PUBLISHING, PUBLISHED, FAILED
        int retryCount,          // 재시도 횟수
        String errorMessage,     // 마지막 에러 메시지
        LocalDateTime publishedAt, // 발행 완료 시간
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static final long[] BACKOFF_MINUTES = {0, 1, 4, 16};

    /**
     * PENDING 상태로 새 OutBox 생성
     */
    public static OrderOutbox pending(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payload,
            UUID correlationId,
            LocalDateTime now
    ) {
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");

        return new OrderOutbox(
                null,  // ID는 저장 시 할당
                aggregateType,
                aggregateId,
                eventType,
                correlationId,
                payload,
                OutboxStatus.PENDING,
                0,
                null,
                null,
                now,
                now
        );
    }

    /**
     * 발행 가능 여부 확인
     *
     * @param maxRetries 최대 재시도 횟수
     * @param now 현재 시간
     * @param clock Clock 인스턴스
     * @return 발행 가능하면 true
     */
    public boolean canPublish(int maxRetries, LocalDateTime now, Clock clock) {
        // 이미 발행 중이거나 완료된 경우
        if (status == OutboxStatus.PUBLISHED || status == OutboxStatus.PUBLISHING) {
            return false;
        }

        // 최대 재시도 횟수 초과
        if (retryCount >= maxRetries) {
            return false;
        }

        // PENDING 상태는 즉시 발행 가능
        if (status == OutboxStatus.PENDING) {
            return true;
        }

        // FAILED 상태는 백오프 시간 경과 확인
        LocalDateTime lastUpdated = updatedAt != null
                ? updatedAt
                : Objects.requireNonNullElseGet(createdAt, () -> LocalDateTime.now(clock));

        LocalDateTime nextRetryAt = lastUpdated.plusMinutes(backoffMinutesForRetry(retryCount));
        return !nextRetryAt.isAfter(now);
    }

    /**
     * 발행 중으로 상태 변경
     */
    public OrderOutbox markPublishing(LocalDateTime now) {
        return new OrderOutbox(
                id,
                aggregateType,
                aggregateId,
                eventType,
                correlationId,
                payload,
                OutboxStatus.PUBLISHING,
                retryCount,
                errorMessage,
                publishedAt,
                createdAt,
                now
        );
    }

    /**
     * 발행 완료로 상태 변경
     */
    public OrderOutbox markPublished(LocalDateTime now) {
        return new OrderOutbox(
                id,
                aggregateType,
                aggregateId,
                eventType,
                correlationId,
                payload,
                OutboxStatus.PUBLISHED,
                retryCount,
                null,  // 에러 메시지 초기화
                now,
                createdAt,
                now
        );
    }

    /**
     * 발행 실패로 상태 변경
     */
    public OrderOutbox markFailed(String error, int maxRetries, LocalDateTime now) {
        return new OrderOutbox(
                id,
                aggregateType,
                aggregateId,
                eventType,
                correlationId,
                payload,
                OutboxStatus.FAILED,
                Math.min(retryCount + 1, maxRetries),
                truncateErrorMessage(error),
                null,  // publishedAt 초기화
                createdAt,
                now
        );
    }

    /**
     * 재시도 횟수에 따른 백오프 시간(분) 계산
     */
    private long backoffMinutesForRetry(int retry) {
        int index = Math.min(retry, BACKOFF_MINUTES.length - 1);
        return BACKOFF_MINUTES[index];
    }

    /**
     * 에러 메시지를 1000자로 제한
     */
    private String truncateErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
