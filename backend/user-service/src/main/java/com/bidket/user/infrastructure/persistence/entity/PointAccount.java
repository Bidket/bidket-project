package com.bidket.user.infrastructure.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import com.bidket.user.domain.model.PointAccountStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_point_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointAccount extends BaseEntity {

    @Id
    @Column(name = "user_id", columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "balance", nullable = false)
    private Long balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PointAccountStatus status;

    @Builder
    public PointAccount(UUID userId, Long balance, PointAccountStatus status) {
        this.userId = userId;
        this.balance = balance != null ? balance : 0L;
        this.status = status != null ? status : PointAccountStatus.ACTIVE;
    }

    public void addBalance(Long amount) {
        this.balance += amount;
    }

    public void subtractBalance(Long amount) {
        if (this.balance < amount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        this.balance -= amount;
    }

    public void suspend() {
        this.status = PointAccountStatus.SUSPENDED;
    }

    public void activate() {
        this.status = PointAccountStatus.ACTIVE;
    }

    public boolean hasEnoughBalance(Long amount) {
        return this.balance >= amount;
    }
}

