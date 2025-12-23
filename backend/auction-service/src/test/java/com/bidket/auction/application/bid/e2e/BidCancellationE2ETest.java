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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("입찰 취소 E2E 테스트")
class BidCancellationE2ETest {

    @Autowired
    private BidService bidService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    private UUID sellerId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("OUTBID 상태의 입찰은 취소할 수 있어야 함")
    void shouldCancelOutbidBid() {
         
        UUID auctionId = createActiveAuction();
        UUID bidder1 = UUID.randomUUID();
        UUID bidder2 = UUID.randomUUID();

        Bid bid1 = bidService.placeBid(auctionId, bidder1, 310000L);
        bidService.placeBid(auctionId, bidder2, 320000L);  

        Bid outbidBid = bidRepository.findById(bid1.getId()).orElseThrow();
        assertThat(outbidBid.getStatus()).isEqualTo(BidStatus.OUTBID);

        bidService.cancelBid(bid1.getId(), bidder1);

        Bid canceledBid = bidRepository.findById(bid1.getId()).orElseThrow();
        assertThat(canceledBid.getStatus()).isEqualTo(BidStatus.CANCELLED);
    }

    @Test
    @DisplayName("ACTIVE(최고가) 입찰은 취소할 수 없어야 함")
    void shouldNotCancelActiveBid() {
         
        UUID auctionId = createActiveAuction();
        UUID bidderId = UUID.randomUUID();

        Bid activeBid = bidService.placeBid(auctionId, bidderId, 310000L);

        assertThatThrownBy(() -> bidService.cancelBid(activeBid.getId(), bidderId))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("WON 상태의 입찰은 취소할 수 없어야 함")
    void shouldNotCancelWonBid() {
         
        UUID auctionId = createActiveAuction();
        UUID bidderId = UUID.randomUUID();

        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(350000L)
                .status(BidStatus.WON)
                .isHighest(true)
                .build();

        Bid wonBid = bidRepository.save(bid);

        assertThatThrownBy(() -> bidService.cancelBid(wonBid.getId(), bidderId))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("본인의 입찰만 취소할 수 있어야 함")
    void shouldOnlyCancelOwnBid() {
         
        UUID auctionId = createActiveAuction();
        UUID bidder1 = UUID.randomUUID();
        UUID bidder2 = UUID.randomUUID();

        Bid bid1 = bidService.placeBid(auctionId, bidder1, 310000L);
        bidService.placeBid(auctionId, bidder2, 320000L);

        Bid outbidBid = bidRepository.findById(bid1.getId()).orElseThrow();

        assertThatThrownBy(() -> bidService.cancelBid(outbidBid.getId(), bidder2))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("취소된 입찰은 isHighest가 false여야 함")
    void shouldNotBeHighestAfterCancellation() {
         
        UUID auctionId = createActiveAuction();
        UUID bidder1 = UUID.randomUUID();
        UUID bidder2 = UUID.randomUUID();

        Bid bid1 = bidService.placeBid(auctionId, bidder1, 310000L);
        bidService.placeBid(auctionId, bidder2, 320000L);

        bidService.cancelBid(bid1.getId(), bidder1);

        Bid canceledBid = bidRepository.findById(bid1.getId()).orElseThrow();
        assertThat(canceledBid.isHighest()).isFalse();
        assertThat(canceledBid.getStatus()).isEqualTo(BidStatus.CANCELLED);
    }

    private UUID createActiveAuction() {
        PriceInfo priceInfo = PriceInfo.builder()
                .startPrice(300000L)
                .currentPrice(300000L)
                .bidIncrement(10000L)
                .buyNowPrice(500000L)
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
                .auctionTitle("입찰 취소 테스트 경매")
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
