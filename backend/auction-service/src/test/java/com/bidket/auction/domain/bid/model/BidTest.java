package com.bidket.auction.domain.bid.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Bid 도메인 모델 테스트")
class BidTest {

    @Test
    @DisplayName("입찰을 생성할 수 있다")
    void shouldCreateBid() {
        // Given
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        Long amount = 350000L;

        // When
        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(amount)
                .build();

        // Then
        assertThat(bid).isNotNull();
        assertThat(bid.getAuctionId()).isEqualTo(auctionId);
        assertThat(bid.getBidderId()).isEqualTo(bidderId);
        assertThat(bid.getAmount()).isEqualTo(amount);
        assertThat(bid.getStatus()).isEqualTo(BidStatus.PENDING);
        assertThat(bid.isHighest()).isFalse();
    }

    @Test
    @DisplayName("입찰 금액은 0보다 커야 한다")
    void shouldValidateBidAmount() {
        // Given
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> 
            Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(0L)
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("입찰 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("입찰을 최고가로 설정할 수 있다")
    void shouldMarkAsHighest() {
        // Given
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();

        // When
        bid.markAsHighest();

        // Then
        assertThat(bid.isHighest()).isTrue();
        assertThat(bid.getStatus()).isEqualTo(BidStatus.ACTIVE);
    }

    @Test
    @DisplayName("최고가 입찰이 밀릴 수 있다")
    void shouldMarkAsOutbid() {
        // Given
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();
        bid.markAsHighest();

        // When
        bid.markAsOutbid();

        // Then
        assertThat(bid.isHighest()).isFalse();
        assertThat(bid.getStatus()).isEqualTo(BidStatus.OUTBID);
    }

    @Test
    @DisplayName("입찰을 낙찰 상태로 변경할 수 있다")
    void shouldMarkAsWon() {
        // Given
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();
        bid.markAsHighest();

        // When
        bid.markAsWon();

        // Then
        assertThat(bid.getStatus()).isEqualTo(BidStatus.WON);
    }

    @Test
    @DisplayName("입찰을 취소할 수 있다")
    void shouldCancelBid() {
        // Given
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();

        // When
        bid.cancel();

        // Then
        assertThat(bid.getStatus()).isEqualTo(BidStatus.CANCELLED);
    }

    @Test
    @DisplayName("최고가 입찰은 취소할 수 없다")
    void shouldNotCancelHighestBid() {
        // Given
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();
        bid.markAsHighest();

        // When & Then
        assertThatThrownBy(() -> bid.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("최고가 입찰은 취소할 수 없습니다");
    }
}

