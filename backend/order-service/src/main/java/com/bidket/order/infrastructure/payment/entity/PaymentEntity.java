package com.bidket.order.infrastructure.payment.entity;

import com.bidket.common.infra.BaseEntity;
import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.model.PaymentMethod;
import com.bidket.order.domain.payment.model.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity extends BaseEntity {

    @Id
    private UUID id;

    private UUID userId;
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private Long amount;
    private Long usedPointAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PaymentEntity(
            UUID id,
            UUID userId,
            UUID orderId,
            PaymentMethod method,
            Long amount,
            Long usedPointAmount,
            PaymentStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.orderId = orderId;
        this.method = method;
        this.amount = amount;
        this.usedPointAmount = usedPointAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentEntity from(Payment payment) {
        return new PaymentEntity(
                payment.id(),
                payment.userId(),
                payment.orderId(),
                payment.method(),
                payment.amount(),
                payment.usedPointAmount(),
                payment.status(),
                payment.createdAt(),
                payment.updatedAt()
        );
    }

    public Payment toModel() {
        return new Payment(
                id,
                userId,
                orderId,
                method,
                amount,
                usedPointAmount,
                status,
                createdAt,
                updatedAt
        );
    }
}