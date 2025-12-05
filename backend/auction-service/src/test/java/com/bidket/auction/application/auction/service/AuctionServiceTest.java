package com.bidket.auction.application.auction.service;

import com.bidket.auction.application.auction.dto.request.CreateAuctionRequest;
import com.bidket.auction.application.auction.dto.request.UpdateAuctionRequest;
import com.bidket.auction.application.auction.dto.response.AuctionResponse;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.model.vo.AuctionPeriod;
import com.bidket.auction.domain.auction.model.vo.AuctionStats;
import com.bidket.auction.domain.auction.model.vo.PriceInfo;
import com.bidket.auction.domain.auction.model.vo.WinnerInfo;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.auction.service.AuctionValidator;
import com.bidket.auction.infrastructure.redis.ViewCountCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionService 단위 테스트")
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionValidator auctionValidator;

    @Mock
    private ViewCountCacheService viewCountCacheService;

    @InjectMocks
    private AuctionService auctionService;

    private UUID testAuctionId;
    private UUID testProductSizeId;
    private UUID testSellerId;
    private Auction testAuction;

    @BeforeEach
    void setUp() {
        testAuctionId = UUID.randomUUID();
        testProductSizeId = UUID.randomUUID();
        testSellerId = UUID.randomUUID();

        PriceInfo priceInfo = PriceInfo.builder()
                .startPrice(250000L)
                .currentPrice(250000L)
                .bidIncrement(10000L)
                .buyNowPrice(400000L)
                .build();

        AuctionPeriod period = AuctionPeriod.builder()
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusDays(2))
                .originalEndTime(LocalDateTime.now().plusDays(2))
                .extensionCount(0)
                .build();

        testAuction = Auction.builder()
                .productSizeId(testProductSizeId)
                .sellerId(testSellerId)
                .auctionTitle("[새제품] Nike Air Jordan 1")
                .description("새 제품입니다")
                .condition(AuctionCondition.DEADSTOCK)
                .priceInfo(priceInfo)
                .period(period)
                .stats(AuctionStats.createDefault())
                .winnerInfo(WinnerInfo.empty())
                .status(AuctionStatus.CREATING)
                .build();
    }

    @Nested
    @DisplayName("경매 생성 테스트")
    class CreateAuctionTest {

        @Test
        @DisplayName("성공: 유효한 요청으로 경매 생성")
        void createAuction_Success() {
            // Given
            CreateAuctionRequest request = new CreateAuctionRequest(
                    testProductSizeId,
                    testSellerId,
                    "[새제품] Nike Air Jordan 1",
                    "새 제품입니다",
                    AuctionCondition.DEADSTOCK,
                    250000L,
                    10000L,
                    400000L,
                    LocalDateTime.now().plusHours(2),
                    LocalDateTime.now().plusDays(2)
            );

            given(auctionRepository.save(any(Auction.class)))
                    .willReturn(testAuction);

            // When
            AuctionResponse response = auctionService.createAuction(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.auctionTitle()).isEqualTo("[새제품] Nike Air Jordan 1");
            assertThat(response.status()).isEqualTo(AuctionStatus.CREATING);
            assertThat(response.startPrice()).isEqualTo(250000L);
            assertThat(response.currentPrice()).isEqualTo(250000L);

            verify(auctionValidator).validateCreate(request);
            verify(auctionRepository).save(any(Auction.class));
        }

        @Test
        @DisplayName("실패: 검증 실패 시 예외 발생")
        void createAuction_ValidationFailed() {
            // Given
            CreateAuctionRequest request = new CreateAuctionRequest(
                    testProductSizeId,
                    testSellerId,
                    "짧음", // 5자 미만
                    null,
                    AuctionCondition.NEW,
                    250000L,
                    10000L,
                    null,
                    LocalDateTime.now().plusHours(2),
                    LocalDateTime.now().plusDays(2)
            );

            willThrow(new IllegalArgumentException("경매 제목은 5자 이상이어야 합니다"))
                    .given(auctionValidator).validateCreate(request);

            // When & Then
            assertThatThrownBy(() -> auctionService.createAuction(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("경매 제목은 5자 이상이어야 합니다");

            verify(auctionValidator).validateCreate(request);
            verify(auctionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("경매 조회 테스트")
    class GetAuctionTest {

        @Test
        @DisplayName("성공: ID로 경매 조회")
        void getAuction_Success() {
            // Given
            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));
            given(viewCountCacheService.getViewCount(any(UUID.class), any(Integer.class)))
                    .willReturn(0);

            // When
            AuctionResponse response = auctionService.getAuction(testAuctionId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.auctionTitle()).isEqualTo("[새제품] Nike Air Jordan 1");

            verify(auctionRepository).findById(testAuctionId);
            verify(viewCountCacheService).incrementViewCountAsync(testAuctionId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 경매")
        void getAuction_NotFound() {
            // Given
            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> auctionService.getAuction(testAuctionId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("경매를 찾을 수 없습니다");

            verify(auctionRepository).findById(testAuctionId);
        }
    }

    @Nested
    @DisplayName("경매 목록 조회 테스트")
    class GetAuctionsTest {

        @Test
        @DisplayName("성공: 판매자별 경매 목록 조회")
        void getAuctionsBySeller_Success() {
            // Given
            List<Auction> auctions = List.of(testAuction);
            given(auctionRepository.findBySellerId(testSellerId))
                    .willReturn(auctions);

            // When
            List<AuctionResponse> responses = auctionService.getAuctionsBySeller(testSellerId);

            // Then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).sellerId()).isEqualTo(testSellerId);

            verify(auctionRepository).findBySellerId(testSellerId);
        }

        @Test
        @DisplayName("성공: 상태별 경매 목록 조회")
        void getAuctionsByStatus_Success() {
            // Given
            testAuction.confirmCreation();
            testAuction.start();
            
            List<Auction> auctions = List.of(testAuction);
            given(auctionRepository.findByStatus(AuctionStatus.ACTIVE))
                    .willReturn(auctions);

            // When
            List<AuctionResponse> responses = auctionService
                    .getAuctionsByStatus(AuctionStatus.ACTIVE);

            // Then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).status()).isEqualTo(AuctionStatus.ACTIVE);

            verify(auctionRepository).findByStatus(AuctionStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("경매 수정 테스트")
    class UpdateAuctionTest {

        @Test
        @DisplayName("성공: 유효한 수정 요청")
        void updateAuction_Success() {
            // Given
            testAuction.confirmCreation(); // PENDING 상태로 전환

            UpdateAuctionRequest request = new UpdateAuctionRequest(
                    "[수정] 새로운 제목",
                    "새로운 설명",
                    null,
                    null,
                    null
            );

            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));
            given(auctionRepository.save(any(Auction.class)))
                    .willReturn(testAuction);

            // When
            AuctionResponse response = auctionService.updateAuction(testAuctionId, request);

            // Then
            assertThat(response).isNotNull();

            verify(auctionValidator).validateUpdate(testAuctionId, request);
            verify(auctionRepository).findById(testAuctionId);
            verify(auctionRepository).save(testAuction);
        }

        @Test
        @DisplayName("실패: 경매를 찾을 수 없음")
        void updateAuction_NotFound() {
            // Given
            UpdateAuctionRequest request = new UpdateAuctionRequest(
                    "[수정] 새로운 제목",
                    null,
                    null,
                    null,
                    null
            );

            willThrow(new IllegalArgumentException("경매를 찾을 수 없습니다"))
                    .given(auctionValidator).validateUpdate(testAuctionId, request);

            // When & Then
            assertThatThrownBy(() -> auctionService.updateAuction(testAuctionId, request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(auctionValidator).validateUpdate(testAuctionId, request);
        }
    }

    @Nested
    @DisplayName("경매 취소 테스트")
    class CancelAuctionTest {

        @Test
        @DisplayName("성공: PENDING 상태 경매 취소")
        void cancelAuction_PendingStatus_Success() {
            // Given
            testAuction.confirmCreation(); // PENDING 상태로 전환

            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));

            // When
            auctionService.cancelAuction(testAuctionId);

            // Then
            assertThat(testAuction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);

            verify(auctionValidator).validateCancel(testAuctionId);
            verify(auctionRepository).findById(testAuctionId);
            verify(auctionRepository).save(testAuction);
        }

        @Test
        @DisplayName("실패: 입찰이 있는 경매 취소 시도")
        void cancelAuction_WithBids_Fail() {
            // Given
            willThrow(new IllegalStateException("입찰이 있는 경매는 취소할 수 없습니다"))
                    .given(auctionValidator).validateCancel(testAuctionId);

            // When & Then
            assertThatThrownBy(() -> auctionService.cancelAuction(testAuctionId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("입찰이 있는 경매는 취소할 수 없습니다");

            verify(auctionValidator).validateCancel(testAuctionId);
        }
    }

    @Nested
    @DisplayName("경매 확정 테스트")
    class ConfirmAuctionTest {

        @Test
        @DisplayName("성공: CREATING -> PENDING 상태 전환")
        void confirmAuctionCreation_Success() {
            // Given
            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));

            // When
            auctionService.confirmAuctionCreation(testAuctionId);

            // Then
            assertThat(testAuction.getStatus()).isEqualTo(AuctionStatus.PENDING);

            verify(auctionRepository).findById(testAuctionId);
            verify(auctionRepository).save(testAuction);
        }

        @Test
        @DisplayName("실패: CREATING 상태가 아닌 경우")
        void confirmAuctionCreation_InvalidStatus() {
            // Given
            testAuction.confirmCreation(); // 이미 PENDING
            
            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));

            // When & Then
            assertThatThrownBy(() -> auctionService.confirmAuctionCreation(testAuctionId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CREATING 상태에서만");

            verify(auctionRepository).findById(testAuctionId);
        }
    }
}

