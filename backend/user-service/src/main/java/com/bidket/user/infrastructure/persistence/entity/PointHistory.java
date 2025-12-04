package com.bidket.user.infrastructure.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import com.bidket.user.domain.model.PointHistoryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_point_history", indexes = {
        @Index(name = "idx_p_point_history_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private PointHistoryType type;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "order_id", columnDefinition = "UUID")
    private UUID orderId;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Builder
    public PointHistory(UUID userId, Long amount, PointHistoryType type, 
                       String description, UUID orderId, Long balanceAfter) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.orderId = orderId;
        this.balanceAfter = balanceAfter;
    }
}

