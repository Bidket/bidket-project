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
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.BidDomainException;
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

    @Mock
    private com.bidket.auction.infrastructure.notification.NotificationEventProducer notificationEventProducer;

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
         
        Long bidAmount = 350000L;
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(activeAuction));
        when(bidRepository.findHighestBidByAuctionId(auctionId)).thenReturn(Optional.empty());
        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bid result = bidService.placeBid(auctionId, bidderId, bidAmount);

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

        assertThatThrownBy(() -> bidService.placeBid(auctionId, bidderId, 350000L))
                .isInstanceOf(AuctionDomainException.class);
    }

    @Test
    @DisplayName("판매자는 자신의 경매에 입찰할 수 없다")
    void shouldNotPlaceBidOnOwnAuction() {
         
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(activeAuction));

        assertThatThrownBy(() -> bidService.placeBid(auctionId, sellerId, 350000L))
                .isInstanceOf(BidDomainException.class);
    }

    @Test
    @DisplayName("최소 입찰가보다 낮은 금액으로 입찰할 수 없다")
    void shouldNotPlaceBidBelowMinimumAmount() {
         
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(activeAuction));

        assertThatThrownBy(() -> bidService.placeBid(auctionId, bidderId, 300000L))
                .isInstanceOf(BidDomainException.class);
    }

    @Test
    @DisplayName("이전 최고가 입찰을 OUTBID 상태로 변경한다")
    void shouldMarkPreviousHighestBidAsOutbid() {
         
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

        Bid result = bidService.placeBid(auctionId, bidderId, bidAmount);

        assertThat(previousHighestBid.getStatus()).isEqualTo(BidStatus.OUTBID);
        assertThat(previousHighestBid.isHighest()).isFalse();
        assertThat(result.isHighest()).isTrue();
        verify(bidRepository, times(2)).save(any(Bid.class));
    }

    @Test
    @DisplayName("입찰 시 경매의 현재가를 업데이트한다")
    void shouldUpdateAuctionCurrentPrice() {
         
        Long bidAmount = 350000L;
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(activeAuction));
        when(bidRepository.findHighestBidByAuctionId(auctionId)).thenReturn(Optional.empty());
        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bidService.placeBid(auctionId, bidderId, bidAmount);

        verify(auctionRepository).save(argThat(auction -> 
            auction.getPriceInfo().getCurrentPrice().equals(bidAmount)
        ));
    }

    @Test
    @DisplayName("최고가가 아닌 입찰을 취소할 수 있다")
    void shouldCancelBidWhenNotHighest() {
         
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder()
                .id(bidId)
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(310000L)
                .status(BidStatus.OUTBID)
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bidService.cancelBid(bidId, bidderId);

        assertThat(bid.getStatus()).isEqualTo(BidStatus.CANCELLED);
        verify(bidRepository).save(bid);
    }

    @Test
    @DisplayName("최고가 입찰은 취소할 수 없다")
    void shouldNotCancelHighestBid() {
         
        UUID bidId = UUID.randomUUID();
        Bid highestBid = Bid.builder()
                .id(bidId)
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(350000L)
                .build();
        highestBid.markAsHighest();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(highestBid));

        assertThatThrownBy(() -> bidService.cancelBid(bidId, bidderId))
                .isInstanceOf(BidDomainException.class);
    }

    @Test
    @DisplayName("다른 사용자의 입찰은 취소할 수 없다")
    void shouldNotCancelOtherUsersBid() {
         
        UUID bidId = UUID.randomUUID();
        UUID otherBidderId = UUID.randomUUID();
        Bid bid = Bid.builder()
                .id(bidId)
                .auctionId(auctionId)
                .bidderId(otherBidderId)
                .amount(310000L)
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        assertThatThrownBy(() -> bidService.cancelBid(bidId, bidderId))
                .isInstanceOf(BidDomainException.class);
    }

    @Test
    @DisplayName("존재하지 않는 입찰은 취소할 수 없다")
    void shouldNotCancelNonExistentBid() {
         
        UUID bidId = UUID.randomUUID();
        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bidService.cancelBid(bidId, bidderId))
                .isInstanceOf(BidDomainException.class);
    }

    @Test
    @DisplayName("즉시 구매가로 경매를 즉시 낙찰할 수 있다")
    void shouldBuyNow() {
         
        Long buyNowPrice = 500000L;
        PriceInfo priceInfoWithBuyNow = PriceInfo.builder()
                .startPrice(300000L)
                .currentPrice(300000L)
                .bidIncrement(10000L)
                .buyNowPrice(buyNowPrice)
                .build();

        Auction auctionWithBuyNow = Auction.builder()
                .id(auctionId)
                .productSizeId(UUID.randomUUID())
                .sellerId(sellerId)
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .priceInfo(priceInfoWithBuyNow)
                .period(AuctionPeriod.builder()
                        .startTime(LocalDateTime.now().minusHours(1))
                        .endTime(LocalDateTime.now().plusHours(1))
                        .originalEndTime(LocalDateTime.now().plusHours(1))
                        .extensionCount(0)
                        .build())
                .stats(AuctionStats.createDefault())
                .winnerInfo(WinnerInfo.empty())
                .status(AuctionStatus.ACTIVE)
                .build();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auctionWithBuyNow));
        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bid result = bidService.buyNow(auctionId, bidderId);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(buyNowPrice);
        assertThat(result.isHighest()).isTrue();
        assertThat(auctionWithBuyNow.getStatus()).isEqualTo(AuctionStatus.SUCCESS);
        verify(bidRepository).save(any(Bid.class));
        verify(auctionRepository).save(auctionWithBuyNow);
    }

    @Test
    @DisplayName("즉시 구매가가 설정되지 않은 경매는 즉시 구매할 수 없다")
    void shouldNotBuyNowWhenBuyNowPriceNotSet() {
         
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(activeAuction));

        assertThatThrownBy(() -> bidService.buyNow(auctionId, bidderId))
                .isInstanceOf(AuctionDomainException.class);
    }

    @Test
    @DisplayName("판매자는 자신의 경매를 즉시 구매할 수 없다")
    void shouldNotBuyNowOwnAuction() {
         
        Long buyNowPrice = 500000L;
        PriceInfo priceInfoWithBuyNow = PriceInfo.builder()
                .startPrice(300000L)
                .currentPrice(300000L)
                .bidIncrement(10000L)
                .buyNowPrice(buyNowPrice)
                .build();

        Auction auctionWithBuyNow = Auction.builder()
                .id(auctionId)
                .productSizeId(UUID.randomUUID())
                .sellerId(sellerId)
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .priceInfo(priceInfoWithBuyNow)
                .period(AuctionPeriod.builder()
                        .startTime(LocalDateTime.now().minusHours(1))
                        .endTime(LocalDateTime.now().plusHours(1))
                        .originalEndTime(LocalDateTime.now().plusHours(1))
                        .extensionCount(0)
                        .build())
                .stats(AuctionStats.createDefault())
                .winnerInfo(WinnerInfo.empty())
                .status(AuctionStatus.ACTIVE)
                .build();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auctionWithBuyNow));

        assertThatThrownBy(() -> bidService.buyNow(auctionId, sellerId))
                .isInstanceOf(BidDomainException.class);
    }

    @Test
    @DisplayName("ACTIVE 상태가 아닌 경매는 즉시 구매할 수 없다")
    void shouldNotBuyNowWhenAuctionNotActive() {
         
        Long buyNowPrice = 500000L;
        PriceInfo priceInfoWithBuyNow = PriceInfo.builder()
                .startPrice(300000L)
                .currentPrice(300000L)
                .bidIncrement(10000L)
                .buyNowPrice(buyNowPrice)
                .build();

        Auction pendingAuction = Auction.builder()
                .id(auctionId)
                .productSizeId(UUID.randomUUID())
                .sellerId(sellerId)
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .priceInfo(priceInfoWithBuyNow)
                .period(AuctionPeriod.builder()
                        .startTime(LocalDateTime.now().plusHours(1))
                        .endTime(LocalDateTime.now().plusHours(2))
                        .originalEndTime(LocalDateTime.now().plusHours(2))
                        .extensionCount(0)
                        .build())
                .stats(AuctionStats.createDefault())
                .winnerInfo(WinnerInfo.empty())
                .status(AuctionStatus.PENDING)
                .build();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(pendingAuction));

        assertThatThrownBy(() -> bidService.buyNow(auctionId, bidderId))
                .isInstanceOf(AuctionDomainException.class);
    }
}
