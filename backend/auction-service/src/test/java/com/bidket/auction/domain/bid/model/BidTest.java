package com.bidket.auction.domain.bid.model;

import com.bidket.auction.global.exception.BidDomainException;
import com.bidket.auction.global.exception.BidErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Bid 도메인 모델 테스트")
class BidTest {

    @Test
    @DisplayName("입찰을 생성할 수 있다")
    void shouldCreateBid() {
         
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        Long amount = 350000L;

        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(amount)
                .build();

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
         
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        assertThatThrownBy(() -> 
            Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(0L)
                .build()
        ).isInstanceOf(BidDomainException.class)
          .extracting(e -> ((BidDomainException) e).getErrorCode())
          .isEqualTo(BidErrorCode.INVALID_BID_AMOUNT);
    }

    @Test
    @DisplayName("입찰을 최고가로 설정할 수 있다")
    void shouldMarkAsHighest() {
         
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();

        bid.markAsHighest();

        assertThat(bid.isHighest()).isTrue();
        assertThat(bid.getStatus()).isEqualTo(BidStatus.ACTIVE);
    }

    @Test
    @DisplayName("최고가 입찰이 밀릴 수 있다")
    void shouldMarkAsOutbid() {
         
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();
        bid.markAsHighest();

        bid.markAsOutbid();

        assertThat(bid.isHighest()).isFalse();
        assertThat(bid.getStatus()).isEqualTo(BidStatus.OUTBID);
    }

    @Test
    @DisplayName("입찰을 낙찰 상태로 변경할 수 있다")
    void shouldMarkAsWon() {
         
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();
        bid.markAsHighest();

        bid.markAsWon();

        assertThat(bid.getStatus()).isEqualTo(BidStatus.WON);
    }

    @Test
    @DisplayName("입찰을 취소할 수 있다")
    void shouldCancelBid() {
         
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();

        bid.cancel();

        assertThat(bid.getStatus()).isEqualTo(BidStatus.CANCELLED);
    }

    @Test
    @DisplayName("최고가 입찰은 취소할 수 없다")
    void shouldNotCancelHighestBid() {
         
        Bid bid = Bid.builder()
                .auctionId(UUID.randomUUID())
                .bidderId(UUID.randomUUID())
                .amount(350000L)
                .build();
        bid.markAsHighest();

        assertThatThrownBy(() -> bid.cancel())
                .isInstanceOf(BidDomainException.class)
                .extracting(e -> ((BidDomainException) e).getErrorCode())
                .isEqualTo(BidErrorCode.CANNOT_CANCEL_HIGHEST_BID);
    }
}
