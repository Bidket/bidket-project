package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.model.BidStatus;
import com.bidket.auction.domain.bid.repository.BidRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RevertBidStatusCompensationAction 단위 테스트
 * 입찰 상태 복원 로직 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RevertBidStatusCompensationAction 테스트")
class RevertBidStatusCompensationActionTest {

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private RevertBidStatusCompensationAction compensationAction;

    private UUID sagaId;
    private UUID bidId;
    private UUID auctionId;
    private UUID bidderId;
    private Bid bid;

    @BeforeEach
    void setUp() {
        sagaId = UUID.randomUUID();
        bidId = UUID.randomUUID();
        auctionId = UUID.randomUUID();
        bidderId = UUID.randomUUID();

        bid = Bid.builder()
                .id(bidId)
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(150000L)
                .isHighest(true)
                .status(BidStatus.WON)
                .build();
    }

    @Test
    @DisplayName("입찰 상태 복원 성공 (WON → ACTIVE)")
    void execute_Success() throws Exception {
        // given
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(bidRepository.save(any(Bid.class))).thenReturn(bid);

        // when
        compensationAction.execute(sagaId, bidId, "{}");

        // then
        verify(bidRepository).findById(bidId);
        verify(bidRepository).save(bid);
        assertThat(bid.getStatus()).isEqualTo(BidStatus.ACTIVE);
        assertThat(bid.isHighest()).isTrue(); // 여전히 highest는 true
    }

    @Test
    @DisplayName("입찰을 찾을 수 없으면 예외 발생")
    void execute_BidNotFound() {
        // given
        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> compensationAction.execute(sagaId, bidId, "{}"))
                .isInstanceOf(AuctionDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuctionErrorCode.BID_NOT_FOUND);

        verify(bidRepository).findById(bidId);
        verify(bidRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 WON 상태가 아닌 입찰은 건너뜀 (Idempotency)")
    void execute_NotWonStatus_SkipIdempotency() throws Exception {
        // given: 이미 ACTIVE 상태인 입찰
        Bid activeBid = Bid.builder()
                .id(bidId)
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(150000L)
                .isHighest(true)
                .status(BidStatus.ACTIVE)
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(activeBid));

        // when
        compensationAction.execute(sagaId, bidId, "{}");

        // then: 이미 WON이 아니므로 save 호출 없이 건너뜀
        verify(bidRepository).findById(bidId);
        verify(bidRepository, never()).save(any());
        assertThat(activeBid.getStatus()).isEqualTo(BidStatus.ACTIVE);
    }

    @Test
    @DisplayName("CANCELLED 상태 입찰도 건너뜀 (Idempotency)")
    void execute_CancelledStatus_SkipIdempotency() throws Exception {
        // given: CANCELLED 상태인 입찰
        Bid cancelledBid = Bid.builder()
                .id(bidId)
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(150000L)
                .isHighest(false)
                .status(BidStatus.CANCELLED)
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(cancelledBid));

        // when
        compensationAction.execute(sagaId, bidId, "{}");

        // then
        verify(bidRepository).findById(bidId);
        verify(bidRepository, never()).save(any());
        assertThat(cancelledBid.getStatus()).isEqualTo(BidStatus.CANCELLED);
    }

    @Test
    @DisplayName("입찰 상태 복원 후 isHighest는 유지된다")
    void execute_PreservesIsHighest() throws Exception {
        // given
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(bidRepository.save(any(Bid.class))).thenReturn(bid);

        // 초기 상태 확인
        assertThat(bid.isHighest()).isTrue();
        assertThat(bid.getStatus()).isEqualTo(BidStatus.WON);

        // when
        compensationAction.execute(sagaId, bidId, "{}");

        // then
        assertThat(bid.getStatus()).isEqualTo(BidStatus.ACTIVE);
        assertThat(bid.isHighest()).isTrue(); // 여전히 highest
    }
}
