package com.bidket.auction.application.bid.service;

import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.model.BidStatus;
import com.bidket.auction.domain.auction.model.vo.AuctionPeriod;
import com.bidket.auction.domain.auction.model.vo.AuctionStats;
import com.bidket.auction.domain.auction.model.vo.PriceInfo;
import com.bidket.auction.domain.auction.model.vo.WinnerInfo;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.bid.repository.BidRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BidService 테스트")
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private BidService bidService;

    private Auction activeAuction;
    private UUID auctionId;
    private UUID sellerId;
    private UUID bidderId;

    @BeforeEach
    void setUp() {
        auctionId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        bidderId = UUID.randomUUID();

        PriceInfo priceInfo = PriceInfo.builder()
                .startPrice(300000L)
                .currentPrice(300000L)
                .bidIncrement(10000L)
                .build();

        AuctionPeriod period = AuctionPeriod.builder()
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .originalEndTime(LocalDateTime.now().plusHours(1))
                .extensionCount(0)
                .build();

        activeAuction = Auction.builder()
                .id(auctionId)
                .productSizeId(UUID.randomUUID())
                .sellerId(sellerId)
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .priceInfo(priceInfo)
                .period(period)
                .stats(AuctionStats.createDefault())
                .winnerInfo(WinnerInfo.empty())
                .status(AuctionStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("유효한 입찰을 등록할 수 있다")
    void shouldPlaceBid() {
        // Given
        Long bidAmount = 350000L;
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(activeAuction));
        when(bidRepository.findHighestBidByAuctionId(auctionId)).thenReturn(Optional.empty());
        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Bid result = bidService.placeBid(auctionId, bidderId, bidAmount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(bidAmount);
        assertThat(result.isHighest()).isTrue();
        assertThat(result.getStatus()).isEqualTo(BidStatus.ACTIVE);
        verify(bidRepository).save(any(Bid.class));
        verify(auctionRepository).save(activeAuction);
    }

    @Test
    @DisplayName("경매가 ACTIVE 상태가 아니면 입찰할 수 없다")
    void shouldNotPlaceBidWhenAuctionNotActive() {
        // Given
        PriceInfo pendingPriceInfo = PriceInfo.builder()
                .startPrice(300000L)
                .currentPrice(300000L)
                .bidIncrement(10000L)
                .build();

        AuctionPeriod pendingPeriod = AuctionPeriod.builder()
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .originalEndTime(LocalDateTime.now().plusHours(1))
                .extensionCount(0)
                .build();

        activeAuction = Auction.builder()
                .id(auctionId)
                .productSizeId(UUID.randomUUID())
                .sellerId(sellerId)
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .priceInfo(pendingPriceInfo)
                .period(pendingPeriod)
                .stats(AuctionStats.createDefault())
                .winnerInfo(WinnerInfo.empty())
                .status(AuctionStatus.PENDING)
                .build();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(activeAuction));

        // When & Then
        assertThatThrownBy(() -> bidService.placeBid(auctionId, bidderId, 350000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE 상태의 경매에만 입찰할 수 있습니다");
    }

    @Test
    @DisplayName("판매자는 자신의 경매에 입찰할 수 없다")
    void shouldNotPlaceBidOnOwnAuction() {
        // Given
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(activeAuction));

        // When & Then
        assertThatThrownBy(() -> bidService.placeBid(auctionId, sellerId, 350000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 경매에는 입찰할 수 없습니다");
    }

    @Test
    @DisplayName("최소 입찰가보다 낮은 금액으로 입찰할 수 없다")
    void shouldNotPlaceBidBelowMinimumAmount() {
        // Given
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(activeAuction));

        // When & Then
        assertThatThrownBy(() -> bidService.placeBid(auctionId, bidderId, 300000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 입찰가");
    }

    @Test
    @DisplayName("이전 최고가 입찰을 OUTBID 상태로 변경한다")
    void shouldMarkPreviousHighestBidAsOutbid() {
        // Given
        Long bidAmount = 350000L;
        UUID previousBidderId = UUID.randomUUID();
        
        Bid previousHighestBid = Bid.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .bidderId(previousBidderId)
                .amount(310000L)
                .build();
        previousHighestBid.markAsHighest();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(activeAuction));
        when(bidRepository.findHighestBidByAuctionId(auctionId))
                .thenReturn(Optional.of(previousHighestBid));
        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Bid result = bidService.placeBid(auctionId, bidderId, bidAmount);

        // Then
        assertThat(previousHighestBid.getStatus()).isEqualTo(BidStatus.OUTBID);
        assertThat(previousHighestBid.isHighest()).isFalse();
        assertThat(result.isHighest()).isTrue();
        verify(bidRepository, times(2)).save(any(Bid.class));
    }

    @Test
    @DisplayName("입찰 시 경매의 현재가를 업데이트한다")
    void shouldUpdateAuctionCurrentPrice() {
        // Given
        Long bidAmount = 350000L;
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(activeAuction));
        when(bidRepository.findHighestBidByAuctionId(auctionId)).thenReturn(Optional.empty());
        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        bidService.placeBid(auctionId, bidderId, bidAmount);

        // Then
        verify(auctionRepository).save(argThat(auction -> 
            auction.getPriceInfo().getCurrentPrice().equals(bidAmount)
        ));
    }
}

