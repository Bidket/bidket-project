package com.bidket.auction.domain.bid.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BidResult {

    @Column(name = "order_id")
    private UUID orderId;

    public static BidResult empty() {
        return BidResult.builder().build();
    }

    public BidResult withOrderId(UUID orderId) {
        return BidResult.builder()
                .orderId(orderId)
                .build();
    }

    public boolean hasOrder() {
        return orderId != null;
    }
}


