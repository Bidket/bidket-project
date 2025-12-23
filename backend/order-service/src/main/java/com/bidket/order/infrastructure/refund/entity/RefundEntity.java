package com.bidket.order.infrastructure.refund.entity;

import com.bidket.order.domain.refund.model.Refund;
import com.bidket.order.domain.refund.model.RefundStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private UUID paymentId;

    private Long refundAmount;

    private Long refundedPointAmount;

    @Enumerated(EnumType.STRING)
    private RefundStatus status;

    private String reason;

    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    public static RefundEntity from(Refund refund) {
        RefundEntity entity = new RefundEntity();
        entity.id = refund.id();
        entity.userId = refund.userId();
        entity.paymentId = refund.paymentId();
        entity.refundAmount = refund.refundAmount();
        entity.refundedPointAmount = refund.refundedPointAmount();
        entity.status = refund.status();
        entity.reason = refund.reason();
        entity.requestedAt = refund.requestedAt();
        entity.approvedAt = refund.approvedAt();
        return entity;
    }

    public Refund toModel() {
        return new Refund(
                id,
                userId,
                paymentId,
                refundAmount,
                refundedPointAmount,
                status,
                reason,
                requestedAt,
                approvedAt
        );
    }
}