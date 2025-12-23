package com.bidket.auction.application.auction.e2e;

import com.bidket.auction.application.auction.service.AuctionService;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("경매 생성 및 시작 E2E 테스트")
class AuctionCreationE2ETest {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private AuctionRepository auctionRepository;

    private UUID sellerId;
    private UUID productSizeId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        productSizeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("경매를 생성할 수 있어야 함")
    void shouldCreateAuction() {
         
        String auctionTitle = "테스트 경매";
        AuctionCondition condition = AuctionCondition.NEW;
        Long startPrice = 300000L;
        Long bidIncrement = 10000L;
        Long buyNowPrice = 500000L;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.now().plusDays(7);

        Auction auction = Auction.builder()
                .productSizeId(productSizeId)
                .sellerId(sellerId)
                .auctionTitle(auctionTitle)
                .condition(condition)
                .startPrice(startPrice)
                .currentPrice(startPrice)
                .bidIncrement(bidIncrement)
                .buyNowPrice(buyNowPrice)
                .startTime(startTime)
                .endTime(endTime)
                .originalEndTime(endTime)
                .extensionCount(0)
                .status(AuctionStatus.CREATING)
                .build();

        Auction savedAuction = auctionRepository.save(auction);

        assertThat(savedAuction.getId()).isNotNull();
        assertThat(savedAuction.getAuctionTitle()).isEqualTo(auctionTitle);
        assertThat(savedAuction.getStatus()).isEqualTo(AuctionStatus.CREATING);
    }

    @Test
    @DisplayName("경매 생성 후 확인하면 PENDING 상태가 되어야 함")
    void shouldBePendingAfterConfirmation() {
         
        Auction auction = createAuction();

        auction.confirmCreation();
        Auction confirmedAuction = auctionRepository.save(auction);

        assertThat(confirmedAuction.getStatus()).isEqualTo(AuctionStatus.PENDING);
    }

    @Test
    @DisplayName("경매 시작 후 ACTIVE 상태가 되어야 함")
    void shouldBeActiveAfterStart() {
         
        Auction auction = createAuction();
        auction.confirmCreation();

        auction.start();
        Auction startedAuction = auctionRepository.save(auction);

        assertThat(startedAuction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
        assertThat(startedAuction.isActive()).isTrue();
    }

    @Test
    @DisplayName("경매 생성 시 필수 필드가 올바르게 설정되어야 함")
    void shouldHaveRequiredFieldsOnCreation() {
         
        Auction auction = createAuction();
        auction.confirmCreation();
        auction.start();

        Auction savedAuction = auctionRepository.save(auction);

        assertThat(savedAuction.getProductSizeId()).isNotNull();
        assertThat(savedAuction.getSellerId()).isNotNull();
        assertThat(savedAuction.getAuctionTitle()).isNotEmpty();
        assertThat(savedAuction.getPriceInfo().getStartPrice()).isGreaterThan(0);
        assertThat(savedAuction.getPriceInfo().getCurrentPrice()).isEqualTo(savedAuction.getPriceInfo().getStartPrice());
        assertThat(savedAuction.getPeriod().getStartTime()).isNotNull();
        assertThat(savedAuction.getPeriod().getEndTime()).isNotNull();
        assertThat(savedAuction.getPeriod().getEndTime()).isAfter(savedAuction.getPeriod().getStartTime());
    }

    @Test
    @DisplayName("경매 생성 시 초기 통계가 설정되어야 함")
    void shouldHaveInitialStatsOnCreation() {
         
        Auction auction = createAuction();
        Auction savedAuction = auctionRepository.save(auction);

        assertThat(savedAuction.getStats()).isNotNull();
        assertThat(savedAuction.getStats().getTotalBidsCount()).isEqualTo(0);
        assertThat(savedAuction.getStats().getViewCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("경매 생성 시 낙찰자 정보가 비어있어야 함")
    void shouldHaveEmptyWinnerInfoOnCreation() {
         
        Auction auction = createAuction();
        Auction savedAuction = auctionRepository.save(auction);

        assertThat(savedAuction.getWinnerInfo()).isNotNull();
        assertThat(savedAuction.getWinnerInfo().getWinnerId()).isNull();
        assertThat(savedAuction.getWinnerInfo().getWinningBidId()).isNull();
        assertThat(savedAuction.getWinnerInfo().getFinalPrice()).isNull();
    }

    @Test
    @DisplayName("buyNowPrice는 startPrice보다 높아야 함")
    void shouldHaveBuyNowPriceHigherThanStartPrice() {
         
        Long startPrice = 300000L;
        Long buyNowPrice = 500000L;

        Auction auction = Auction.builder()
                .productSizeId(productSizeId)
                .sellerId(sellerId)
                .auctionTitle("즉시 구매 테스트")
                .condition(AuctionCondition.NEW)
                .startPrice(startPrice)
                .currentPrice(startPrice)
                .bidIncrement(10000L)
                .buyNowPrice(buyNowPrice)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(7))
                .originalEndTime(LocalDateTime.now().plusDays(7))
                .extensionCount(0)
                .status(AuctionStatus.CREATING)
                .build();

        Auction savedAuction = auctionRepository.save(auction);

        assertThat(savedAuction.getPriceInfo().getBuyNowPrice()).isGreaterThan(savedAuction.getPriceInfo().getStartPrice());
    }

    private Auction createAuction() {
        return Auction.builder()
                .productSizeId(productSizeId)
                .sellerId(sellerId)
                .auctionTitle("테스트 경매")
                .condition(AuctionCondition.NEW)
                .startPrice(300000L)
                .currentPrice(300000L)
                .bidIncrement(10000L)
                .buyNowPrice(500000L)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(7))
                .originalEndTime(LocalDateTime.now().plusDays(7))
                .extensionCount(0)
                .status(AuctionStatus.CREATING)
                .build();
    }
}
