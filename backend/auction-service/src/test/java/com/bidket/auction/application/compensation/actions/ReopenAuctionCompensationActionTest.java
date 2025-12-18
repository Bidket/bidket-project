package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ReopenAuctionCompensationAction 단위 테스트
 * 경매 재오픈 보상 로직 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReopenAuctionCompensationAction 테스트")
class ReopenAuctionCompensationActionTest {

    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private ReopenAuctionCompensationAction compensationAction;

    private UUID sagaId;
    private UUID auctionId;
    private UUID winnerId;
    private UUID winningBidId;
    private UUID productSizeId;
    private Auction auction;

    @BeforeEach
    void setUp() {
        sagaId = UUID.randomUUID();
        auctionId = UUID.randomUUID();
        winnerId = UUID.randomUUID();
        winningBidId = UUID.randomUUID();
        productSizeId = UUID.randomUUID();

        auction = Auction.builder()
                .id(auctionId)
                .productSizeId(productSizeId)
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .startPrice(100000L)
                .currentPrice(150000L)
                .bidIncrement(10000L)
                .buyNowPrice(null)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().minusHours(1))
                .originalEndTime(LocalDateTime.now().minusDays(1))
                .extensionCount(0)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .finalPrice(150000L)
                .totalBidsCount(5)
                .viewCount(100)
                .status(AuctionStatus.SUCCESS)
                .build();
    }

    @Test
    @DisplayName("경매 재오픈 성공")
    void execute_Success() throws Exception {
        // given
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        // when
        compensationAction.execute(sagaId, auctionId, "{}");

        // then
        verify(auctionRepository).findById(auctionId);
        verify(auctionRepository).save(auction);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
        assertThat(auction.getWinnerInfo().getWinnerId()).isNull();
        assertThat(auction.getWinnerInfo().getWinningBidId()).isNull();
        assertThat(auction.getWinnerInfo().getFinalPrice()).isNull();
    }

    @Test
    @DisplayName("경매를 찾을 수 없으면 예외 발생")
    void execute_AuctionNotFound() {
        // given
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> compensationAction.execute(sagaId, auctionId, "{}"))
                .isInstanceOf(AuctionDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuctionErrorCode.AUCTION_NOT_FOUND);

        verify(auctionRepository).findById(auctionId);
        verify(auctionRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 재오픈된 경매는 건너뜀 (Idempotency)")
    void execute_AlreadyReopened_SkipIdempotency() throws Exception {
        // given: 이미 재오픈된 경매
        Auction reopenedAuction = Auction.builder()
                .id(auctionId)
                .productSizeId(productSizeId)
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .startPrice(100000L)
                .currentPrice(150000L)
                .bidIncrement(10000L)
                .buyNowPrice(null)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().plusHours(23)) // 재오픈으로 연장됨
                .originalEndTime(LocalDateTime.now().minusDays(1))
                .extensionCount(0)
                .winnerId(null)
                .winningBidId(null)
                .finalPrice(null)
                .totalBidsCount(5)
                .viewCount(100)
                .status(AuctionStatus.REOPENED)
                .build();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(reopenedAuction));

        // when
        compensationAction.execute(sagaId, auctionId, "{}");

        // then: 이미 재오픈된 경매는 save 호출 없이 건너뜀
        verify(auctionRepository).findById(auctionId);
        verify(auctionRepository, never()).save(any());
        assertThat(reopenedAuction.getStatus()).isEqualTo(AuctionStatus.REOPENED);
    }

    @Test
    @DisplayName("endTime이 현재 시간으로부터 24시간 연장된다")
    void execute_ExtendEndTime() throws Exception {
        // given
        LocalDateTime originalEndTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        Auction auctionToReopen = Auction.builder()
                .id(auctionId)
                .productSizeId(productSizeId)
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .startPrice(100000L)
                .currentPrice(150000L)
                .bidIncrement(10000L)
                .buyNowPrice(null)
                .startTime(originalEndTime.minusDays(1))
                .endTime(originalEndTime)
                .originalEndTime(originalEndTime)
                .extensionCount(0)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .finalPrice(150000L)
                .totalBidsCount(5)
                .viewCount(100)
                .status(AuctionStatus.SUCCESS)
                .build();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auctionToReopen));
        when(auctionRepository.save(any(Auction.class))).thenReturn(auctionToReopen);

        LocalDateTime beforeReopen = LocalDateTime.now();

        // when
        compensationAction.execute(sagaId, auctionId, "{}");

        // then
        LocalDateTime afterReopen = LocalDateTime.now().plusDays(1);
        assertThat(auctionToReopen.getPeriod().getEndTime()).isAfter(originalEndTime);
        assertThat(auctionToReopen.getPeriod().getEndTime()).isBetween(
                beforeReopen.plusDays(1).minusSeconds(5),
                afterReopen.plusSeconds(5)
        );
        verify(auctionRepository).save(auctionToReopen);
    }

    @Test
    @DisplayName("낙찰자 정보가 초기화된다")
    void execute_ClearWinnerInfo() throws Exception {
        // given
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        // 초기 상태 확인
        assertThat(auction.getWinnerInfo().getWinnerId()).isEqualTo(winnerId);
        assertThat(auction.getWinnerInfo().getWinningBidId()).isEqualTo(winningBidId);
        assertThat(auction.getWinnerInfo().getFinalPrice()).isEqualTo(150000L);

        // when
        compensationAction.execute(sagaId, auctionId, "{}");

        // then
        assertThat(auction.getWinnerInfo().getWinnerId()).isNull();
        assertThat(auction.getWinnerInfo().getWinningBidId()).isNull();
        assertThat(auction.getWinnerInfo().getFinalPrice()).isNull();
    }
}
