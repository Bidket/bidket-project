package com.bidket.order.infrastructure.payment.entity;

import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.model.PaymentMethod;
import com.bidket.order.domain.payment.model.PaymentStatus;
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
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private Long amount;

    private Long usedPointAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static PaymentEntity from(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.orderId = payment.orderId();
        entity.method = payment.method();
        entity.amount = payment.amount();
        entity.usedPointAmount = payment.usedPointAmount();
        entity.status = payment.status();
        entity.createdAt = payment.createdAt();
        entity.updatedAt = payment.updatedAt();
        return entity;
    }

    public Payment toModel() {
        return new Payment(
                id,
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