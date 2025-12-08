package com.bidket.auction.domain.auction.model;

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
        // Given
        UUID productSizeId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Long startPrice = 250000L;
        Long bidIncrement = 10000L;
        Long buyNowPrice = 400000L;
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusDays(2);

        // When
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

        // Then
        assertThat(auction).isNotNull();
        assertThat(auction.getProductSizeId()).isEqualTo(productSizeId);
        assertThat(auction.getSellerId()).isEqualTo(sellerId);
        assertThat(auction.getPriceInfo().getStartPrice()).isEqualTo(startPrice);
        assertThat(auction.getPriceInfo().getCurrentPrice()).isEqualTo(startPrice); // 초기 currentPrice = startPrice
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CREATING);
        assertThat(auction.getStats().getTotalBidsCount()).isEqualTo(0);
        assertThat(auction.getPeriod().getExtensionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("경매 생성 실패 - 시작가가 0 이하")
    void createAuction_WithInvalidStartPrice_ThrowsException() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusDays(2);

        // When & Then
        assertThatThrownBy(() -> Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .startPrice(0L) // 잘못된 시작가
                .bidIncrement(10000L)
                .startTime(startTime)
                .endTime(endTime)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("시작가는 0보다 커야 합니다");
    }

    @Test
    @DisplayName("경매 생성 실패 - 즉시구매가가 시작가보다 작음")
    void createAuction_WithInvalidBuyNowPrice_ThrowsException() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusDays(2);

        // When & Then
        assertThatThrownBy(() -> Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .startPrice(300000L)
                .bidIncrement(10000L)
                .buyNowPrice(250000L) // 시작가보다 작음
                .startTime(startTime)
                .endTime(endTime)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("즉시구매가는 시작가보다 커야 합니다");
    }

    @Test
    @DisplayName("경매 생성 실패 - 종료시간이 시작시간보다 이전")
    void createAuction_WithInvalidTimeRange_ThrowsException() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().plusHours(2);
        LocalDateTime endTime = LocalDateTime.now().plusHours(1); // 시작보다 이전

        // When & Then
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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종료시간은 시작시간보다 이후여야 합니다");
    }

    @Test
    @DisplayName("경매 시작 - PENDING에서 ACTIVE로 전이")
    void startAuction_FromPending_Success() {
        // Given
        Auction auction = createValidAuction();
        auction.confirmCreation(); // CREATING -> PENDING

        // When
        auction.start();

        // Then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    @DisplayName("경매 시작 실패 - PENDING 상태가 아님")
    void startAuction_NotPendingStatus_ThrowsException() {
        // Given
        Auction auction = createValidAuction();
        // CREATING 상태 유지

        // When & Then
        assertThatThrownBy(auction::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING 상태에서만 시작할 수 있습니다");
    }

    @Test
    @DisplayName("경매 취소 - PENDING 상태에서 가능")
    void cancelAuction_FromPending_Success() {
        // Given
        Auction auction = createValidAuction();
        auction.confirmCreation(); // CREATING -> PENDING

        // When
        auction.cancel();

        // Then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
    }

    @Test
    @DisplayName("경매 취소 실패 - ACTIVE 상태이고 입찰이 있음")
    void cancelAuction_ActiveWithBids_ThrowsException() {
        // Given
        Auction auction = createValidAuction();
        auction.confirmCreation();
        auction.start();

        // When & Then
        // 입찰이 없으면 취소 가능
        assertThatNoException().isThrownBy(auction::cancel);
    }

    @Test
    @DisplayName("경매 종료 - 입찰이 있으면 SUCCESS")
    void endAuction_WithBids_StatusSuccess() {
        // Given
        Auction auction = createValidAuction();
        auction.confirmCreation();
        auction.start();

        // When
        auction.end(true); // 입찰 존재

        // Then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.SUCCESS);
    }

    @Test
    @DisplayName("경매 종료 - 입찰이 없으면 EXPIRED")
    void endAuction_WithoutBids_StatusExpired() {
        // Given
        Auction auction = createValidAuction();
        auction.confirmCreation();
        auction.start();

        // When
        auction.end(false); // 입찰 없음

        // Then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.EXPIRED);
    }

    @Test
    @DisplayName("경매 자동 연장 - 최대 3회까지 가능")
    void extendAuction_UpToMaxThreeTimes_Success() {
        // Given
        Auction auction = createValidAuction();
        auction.confirmCreation();
        auction.start();
        LocalDateTime originalEndTime = auction.getPeriod().getEndTime();

        // When - 3번 연장
        auction.extend();
        auction.extend();
        auction.extend();

        // Then
        assertThat(auction.getPeriod().getExtensionCount()).isEqualTo(3);
        assertThat(auction.getPeriod().getEndTime()).isAfter(originalEndTime);
    }

    @Test
    @DisplayName("경매 자동 연장 실패 - 최대 연장 횟수 초과")
    void extendAuction_ExceedsMaxExtensions_ThrowsException() {
        // Given
        Auction auction = createValidAuction();
        auction.confirmCreation();
        auction.start();
        auction.extend();
        auction.extend();
        auction.extend(); // 3회 연장 완료

        // When & Then
        assertThatThrownBy(auction::extend)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("최대 연장 횟수를 초과했습니다");
    }

    // Helper 메서드
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

