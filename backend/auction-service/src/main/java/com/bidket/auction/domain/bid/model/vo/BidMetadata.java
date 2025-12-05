package com.bidket.auction.domain.bid.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BidMetadata {

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    public static BidMetadata empty() {
        return BidMetadata.builder().build();
    }

    public static BidMetadata withIdempotencyKey(String idempotencyKey) {
        return BidMetadata.builder()
                .idempotencyKey(idempotencyKey)
                .build();
    }

    public boolean hasIdempotencyKey() {
        return idempotencyKey != null && !idempotencyKey.isBlank();
    }
}


