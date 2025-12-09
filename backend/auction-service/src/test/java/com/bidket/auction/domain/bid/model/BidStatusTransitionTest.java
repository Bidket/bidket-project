package com.bidket.auction.domain.bid.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BID-007: 입찰 상태 전이 로직 테스트")
class BidStatusTransitionTest {

    @Test
    @DisplayName("PENDING 상태에서 ACTIVE로 전이할 수 있다 (최고가 설정)")
    void shouldTransitionFromPendingToActive() {
        // Given
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();

        assertThat(bid.getStatus()).isEqualTo(BidStatus.PENDING);

        // When
        bid.markAsHighest();

        // Then
        assertThat(bid.getStatus()).isEqualTo(BidStatus.ACTIVE);
        assertThat(bid.isHighest()).isTrue();
    }

    @Test
    @DisplayName("ACTIVE 상태에서 OUTBID로 전이할 수 있다 (최고가 밀림)")
    void shouldTransitionFromActiveToOutbid() {
        // Given
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();
        bid.markAsHighest();

        assertThat(bid.getStatus()).isEqualTo(BidStatus.ACTIVE);
        assertThat(bid.isHighest()).isTrue();

        // When
        bid.markAsOutbid();

        // Then
        assertThat(bid.getStatus()).isEqualTo(BidStatus.OUTBID);
        assertThat(bid.isHighest()).isFalse();
    }

    @Test
    @DisplayName("ACTIVE 상태에서 WON으로 전이할 수 있다 (낙찰)")
    void shouldTransitionFromActiveToWon() {
        // Given
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();
        bid.markAsHighest();

        assertThat(bid.getStatus()).isEqualTo(BidStatus.ACTIVE);

        // When
        bid.markAsWon();

        // Then
        assertThat(bid.getStatus()).isEqualTo(BidStatus.WON);
    }

    @Test
    @DisplayName("ACTIVE가 아닌 상태에서 WON으로 전이할 수 없다")
    void shouldNotTransitionToWonFromNonActiveStatus() {
        // Given
        Bid outbidBid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .status(BidStatus.OUTBID)
                .build();

        // When & Then
        assertThatThrownBy(outbidBid::markAsWon)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE 상태에서만 낙찰될 수 있습니다");
    }

    @Test
    @DisplayName("OUTBID 상태에서 CANCELLED로 전이할 수 있다")
    void shouldTransitionFromOutbidToCancelled() {
        // Given
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .status(BidStatus.OUTBID)
                .build();

        // When
        bid.cancel();

        // Then
        assertThat(bid.getStatus()).isEqualTo(BidStatus.CANCELLED);
    }

    @Test
    @DisplayName("최고가 입찰(isHighest=true)은 취소할 수 없다")
    void shouldNotCancelHighestBid() {
        // Given
        Bid highestBid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();
        highestBid.markAsHighest();

        // When & Then
        assertThatThrownBy(highestBid::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("최고가 입찰은 취소할 수 없습니다");
    }

    @Test
    @DisplayName("PENDING 상태에서 REJECTED로 전이할 수 있다 (검증 실패)")
    void shouldTransitionFromPendingToRejected() {
        // Given
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();

        assertThat(bid.getStatus()).isEqualTo(BidStatus.PENDING);

        // When
        bid.reject();

        // Then
        assertThat(bid.getStatus()).isEqualTo(BidStatus.REJECTED);
    }

    @Test
    @DisplayName("입찰 상태는 비즈니스 로직에 따라 올바르게 전이되어야 한다")
    void shouldFollowCorrectStateTransitions() {
        // Given
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();

        assertThat(bid.getStatus()).isEqualTo(BidStatus.PENDING);

        bid.markAsHighest();
        assertThat(bid.getStatus()).isEqualTo(BidStatus.ACTIVE);

        bid.markAsOutbid();
        assertThat(bid.getStatus()).isEqualTo(BidStatus.OUTBID);

        bid.cancel();
        assertThat(bid.getStatus()).isEqualTo(BidStatus.CANCELLED);
    }

    @Test
    @DisplayName("낙찰 시나리오: PENDING → ACTIVE → WON")
    void shouldTransitionInWinningScenario() {
        // Given
        Bid winningBid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(500000L)
                .build();

        // When & Then
        assertThat(winningBid.getStatus()).isEqualTo(BidStatus.PENDING);

        winningBid.markAsHighest();
        assertThat(winningBid.getStatus()).isEqualTo(BidStatus.ACTIVE);
        assertThat(winningBid.isHighest()).isTrue();

        winningBid.markAsWon();
        assertThat(winningBid.getStatus()).isEqualTo(BidStatus.WON);
    }

    @Test
    @DisplayName("밀린 입찰 시나리오: PENDING → ACTIVE → OUTBID")
    void shouldTransitionInOutbidScenario() {
        // Given
        Bid outbidBid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();

        // When & Then
        assertThat(outbidBid.getStatus()).isEqualTo(BidStatus.PENDING);

        outbidBid.markAsHighest();
        assertThat(outbidBid.getStatus()).isEqualTo(BidStatus.ACTIVE);

        outbidBid.markAsOutbid();
        assertThat(outbidBid.getStatus()).isEqualTo(BidStatus.OUTBID);
        assertThat(outbidBid.isHighest()).isFalse();
    }

    @Test
    @DisplayName("입찰 검증 실패 시나리오: PENDING → REJECTED")
    void shouldTransitionInRejectionScenario() {
        // Given
        Bid rejectedBid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(100L)
                .build();

        // When & Then
        assertThat(rejectedBid.getStatus()).isEqualTo(BidStatus.PENDING);

        rejectedBid.reject();
        assertThat(rejectedBid.getStatus()).isEqualTo(BidStatus.REJECTED);
    }

    @Test
    @DisplayName("입찰 취소 시나리오: OUTBID → CANCELLED")
    void shouldTransitionInCancellationScenario() {
        // Given
        Bid cancelledBid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .status(BidStatus.OUTBID)
                .build();

        // When & Then
        assertThat(cancelledBid.getStatus()).isEqualTo(BidStatus.OUTBID);
        assertThat(cancelledBid.isHighest()).isFalse();

        cancelledBid.cancel();
        assertThat(cancelledBid.getStatus()).isEqualTo(BidStatus.CANCELLED);
    }
}

