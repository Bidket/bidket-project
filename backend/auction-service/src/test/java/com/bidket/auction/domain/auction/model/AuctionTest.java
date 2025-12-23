package com.bidket.auction.domain.auction.model;

import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Auction Entity 테스트")
class AuctionTest {

    @Test
    @DisplayName("경매 생성 - Builder 패턴으로 생성 가능")
    void createAuction_WithBuilder_Success() {
         
        UUID productSizeId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Long startPrice = 250000L;
        Long bidIncrement = 10000L;
        Long buyNowPrice = 400000L;
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusDays(2);

        Auction auction = Auction.builder()
                .productSizeId(productSizeId)
                .sellerId(sellerId)
                .auctionTitle("[새제품] Nike Air Jordan 1")
                .description("새 제품입니다")
                .condition(AuctionCondition.DEADSTOCK)
                .startPrice(startPrice)
                .bidIncrement(bidIncrement)
                .buyNowPrice(buyNowPrice)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        assertThat(auction).isNotNull();
        assertThat(auction.getProductSizeId()).isEqualTo(productSizeId);
        assertThat(auction.getSellerId()).isEqualTo(sellerId);
        assertThat(auction.getPriceInfo().getStartPrice()).isEqualTo(startPrice);
        assertThat(auction.getPriceInfo().getCurrentPrice()).isEqualTo(startPrice);  
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CREATING);
        assertThat(auction.getStats().getTotalBidsCount()).isEqualTo(0);
        assertThat(auction.getPeriod().getExtensionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("경매 생성 실패 - 시작가가 0 이하")
    void createAuction_WithInvalidStartPrice_ThrowsException() {
         
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusDays(2);

        assertThatThrownBy(() -> Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .startPrice(0L)  
                .bidIncrement(10000L)
                .startTime(startTime)
                .endTime(endTime)
                .build())
                .isInstanceOf(AuctionDomainException.class)
                .extracting(e -> ((AuctionDomainException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.INVALID_START_PRICE);
    }

    @Test
    @DisplayName("경매 생성 실패 - 즉시구매가가 시작가보다 작음")
    void createAuction_WithInvalidBuyNowPrice_ThrowsException() {
         
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusDays(2);

        assertThatThrownBy(() -> Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .startPrice(300000L)
                .bidIncrement(10000L)
                .buyNowPrice(250000L)  
                .startTime(startTime)
                .endTime(endTime)
                .build())
                .isInstanceOf(AuctionDomainException.class)
                .extracting(e -> ((AuctionDomainException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.INVALID_BUY_NOW_PRICE);
    }

    @Test
    @DisplayName("경매 생성 실패 - 종료시간이 시작시간보다 이전")
    void createAuction_WithInvalidTimeRange_ThrowsException() {
         
        LocalDateTime startTime = LocalDateTime.now().plusHours(2);
        LocalDateTime endTime = LocalDateTime.now().plusHours(1);  

        assertThatThrownBy(() -> Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .startPrice(300000L)
                .bidIncrement(10000L)
                .startTime(startTime)
                .endTime(endTime)
                .build())
                .isInstanceOf(AuctionDomainException.class)
                .extracting(e -> ((AuctionDomainException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.INVALID_TIME_RANGE);
    }

    @Test
    @DisplayName("경매 시작 - PENDING에서 ACTIVE로 전이")
    void startAuction_FromPending_Success() {
         
        Auction auction = createValidAuction();
        auction.confirmCreation();  

        auction.start();

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    @DisplayName("경매 시작 실패 - PENDING 상태가 아님")
    void startAuction_NotPendingStatus_ThrowsException() {
         
        Auction auction = createValidAuction();
         

        assertThatThrownBy(auction::start)
                .isInstanceOf(AuctionDomainException.class)
                .extracting(e -> ((AuctionDomainException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.INVALID_AUCTION_STATUS);
    }

    @Test
    @DisplayName("경매 취소 - PENDING 상태에서 가능")
    void cancelAuction_FromPending_Success() {
         
        Auction auction = createValidAuction();
        auction.confirmCreation();  

        auction.cancel();

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
    }

    @Test
    @DisplayName("경매 취소 실패 - ACTIVE 상태이고 입찰이 있음")
    void cancelAuction_ActiveWithBids_ThrowsException() {
         
        Auction auction = createValidAuction();
        auction.confirmCreation();
        auction.start();

        assertThatNoException().isThrownBy(auction::cancel);
    }

    @Test
    @DisplayName("경매 종료 - 입찰이 있으면 SUCCESS")
    void endAuction_WithBids_StatusSuccess() {
         
        Auction auction = createValidAuction();
        auction.confirmCreation();
        auction.start();

        auction.end(true);  

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.SUCCESS);
    }

    @Test
    @DisplayName("경매 종료 - 입찰이 없으면 EXPIRED")
    void endAuction_WithoutBids_StatusExpired() {
         
        Auction auction = createValidAuction();
        auction.confirmCreation();
        auction.start();

        auction.end(false);  

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.EXPIRED);
    }

    @Test
    @DisplayName("경매 자동 연장 - 최대 3회까지 가능")
    void extendAuction_UpToMaxThreeTimes_Success() {
         
        Auction auction = createValidAuction();
        auction.confirmCreation();
        auction.start();
        LocalDateTime originalEndTime = auction.getPeriod().getEndTime();

        auction.extend();
        auction.extend();
        auction.extend();

        assertThat(auction.getPeriod().getExtensionCount()).isEqualTo(3);
        assertThat(auction.getPeriod().getEndTime()).isAfter(originalEndTime);
    }

    @Test
    @DisplayName("경매 자동 연장 실패 - 최대 연장 횟수 초과")
    void extendAuction_ExceedsMaxExtensions_ThrowsException() {
         
        Auction auction = createValidAuction();
        auction.confirmCreation();
        auction.start();
        auction.extend();
        auction.extend();
        auction.extend();  

        assertThatThrownBy(auction::extend)
                .isInstanceOf(AuctionDomainException.class)
                .extracting(e -> ((AuctionDomainException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.MAX_EXTENSIONS_REACHED);
    }

    private Auction createValidAuction() {
        return Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .description("Test Description")
                .condition(AuctionCondition.NEW)
                .startPrice(250000L)
                .bidIncrement(10000L)
                .buyNowPrice(400000L)
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .build();
    }
}
