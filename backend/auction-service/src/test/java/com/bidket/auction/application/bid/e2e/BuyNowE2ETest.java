package com.bidket.auction.application.bid.e2e;

import com.bidket.auction.application.bid.service.BidService;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.model.vo.AuctionPeriod;
import com.bidket.auction.domain.auction.model.vo.AuctionStats;
import com.bidket.auction.domain.auction.model.vo.PriceInfo;
import com.bidket.auction.domain.auction.model.vo.WinnerInfo;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.model.BidStatus;
import com.bidket.auction.domain.bid.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("즉시 구매 E2E 테스트")
class BuyNowE2ETest {

    @Autowired
    private BidService bidService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    private UUID sellerId;
    private UUID buyerId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("즉시 구매 시 경매가 즉시 종료되어야 함")
    void shouldCloseAuctionImmediatelyOnBuyNow() {
         
        UUID auctionId = createAuctionWithBuyNowPrice(500000L);

        Bid buyNowBid = bidService.buyNow(auctionId, buyerId);

        assertThat(buyNowBid).isNotNull();
        assertThat(buyNowBid.getAmount()).isEqualTo(500000L);
        assertThat(buyNowBid.getStatus()).isEqualTo(BidStatus.ACTIVE);
        assertThat(buyNowBid.isHighest()).isTrue();

        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(auction.getStatus()).isIn(AuctionStatus.SUCCESS, AuctionStatus.PAYMENT_PENDING);
        assertThat(auction.getPriceInfo().getCurrentPrice()).isEqualTo(500000L);
    }

    @Test
    @DisplayName("즉시 구매 전에 다른 입찰이 있는 경우 모두 OUTBID 처리")
    void shouldMarkPreviousBidsAsOutbid() {
         
        UUID auctionId = createAuctionWithBuyNowPrice(500000L);
        UUID bidder1 = UUID.randomUUID();
        UUID bidder2 = UUID.randomUUID();

        bidService.placeBid(auctionId, bidder1, 310000L);
        bidService.placeBid(auctionId, bidder2, 320000L);

        Bid buyNowBid = bidService.buyNow(auctionId, buyerId);

        List<Bid> allBids = bidRepository.findByAuctionId(auctionId);
        long outbidCount = allBids.stream()
                .filter(bid -> !bid.getId().equals(buyNowBid.getId()))
                .filter(bid -> bid.getStatus() == BidStatus.OUTBID)
                .count();

        assertThat(outbidCount).isEqualTo(2);

        assertThat(buyNowBid.isHighest()).isTrue();
        assertThat(buyNowBid.getStatus()).isEqualTo(BidStatus.ACTIVE);
    }

    @Test
    @DisplayName("즉시 구매 후 경매에 낙찰 정보가 설정되어야 함")
    void shouldSetWinnerInfoAfterBuyNow() {
         
        UUID auctionId = createAuctionWithBuyNowPrice(500000L);

        Bid buyNowBid = bidService.buyNow(auctionId, buyerId);

        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(auction.getWinnerInfo()).isNotNull();
         
        assertThat(auction.getStatus()).isIn(AuctionStatus.SUCCESS, AuctionStatus.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("즉시 구매 가격은 buyNowPrice와 동일해야 함")
    void shouldMatchBuyNowPrice() {
         
        Long buyNowPrice = 500000L;
        UUID auctionId = createAuctionWithBuyNowPrice(buyNowPrice);

        Bid buyNowBid = bidService.buyNow(auctionId, buyerId);

        assertThat(buyNowBid.getAmount()).isEqualTo(buyNowPrice);

        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(auction.getPriceInfo().getCurrentPrice()).isEqualTo(buyNowPrice);
    }

    @Test
    @DisplayName("즉시 구매 후 더 이상 입찰할 수 없어야 함")
    void shouldNotAllowBidsAfterBuyNow() {
         
        UUID auctionId = createAuctionWithBuyNowPrice(500000L);
        bidService.buyNow(auctionId, buyerId);

        Auction auction = auctionRepository.findById(auctionId).orElseThrow();

        assertThat(auction.getStatus()).isIn(AuctionStatus.SUCCESS, AuctionStatus.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("즉시 구매 시 현재 최고가보다 높은 가격으로 입찰됨")
    void shouldBuyNowWithPriceHigherThanCurrent() {
         
        UUID auctionId = createAuctionWithBuyNowPrice(500000L);
        bidService.placeBid(auctionId, UUID.randomUUID(), 320000L);

        Auction auctionBefore = auctionRepository.findById(auctionId).orElseThrow();
        Long currentPriceBefore = auctionBefore.getPriceInfo().getCurrentPrice();

        Bid buyNowBid = bidService.buyNow(auctionId, buyerId);

        assertThat(buyNowBid.getAmount()).isGreaterThan(currentPriceBefore);
        assertThat(buyNowBid.getAmount()).isEqualTo(500000L);
    }

    private UUID createAuctionWithBuyNowPrice(Long buyNowPrice) {
        PriceInfo priceInfo = PriceInfo.builder()
                .startPrice(300000L)
                .currentPrice(300000L)
                .bidIncrement(10000L)
                .buyNowPrice(buyNowPrice)
                .build();

        AuctionPeriod period = AuctionPeriod.builder()
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(24))
                .originalEndTime(LocalDateTime.now().plusHours(24))
                .extensionCount(0)
                .build();

        Auction auction = Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(sellerId)
                .auctionTitle("즉시 구매 테스트 경매")
                .condition(AuctionCondition.NEW)
                .priceInfo(priceInfo)
                .period(period)
                .stats(AuctionStats.createDefault())
                .winnerInfo(WinnerInfo.empty())
                .status(AuctionStatus.CREATING)
                .build();

        auction.confirmCreation();
        auction.start();

        Auction savedAuction = auctionRepository.save(auction);
        return savedAuction.getId();
    }
}
