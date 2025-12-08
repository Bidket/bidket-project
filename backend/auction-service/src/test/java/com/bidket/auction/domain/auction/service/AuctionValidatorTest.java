package com.bidket.auction.domain.auction.service;

import com.bidket.auction.application.auction.dto.request.CreateAuctionRequest;
import com.bidket.auction.application.auction.dto.request.UpdateAuctionRequest;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionValidator 단위 테스트")
class AuctionValidatorTest {

    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private AuctionValidator auctionValidator;

    private UUID testAuctionId;
    private UUID testProductSizeId;
    private UUID testSellerId;

    @BeforeEach
    void setUp() {
        testAuctionId = UUID.randomUUID();
        testProductSizeId = UUID.randomUUID();
        testSellerId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("경매 생성 검증")
    class ValidateCreateTest {

        @Test
        @DisplayName("성공: 유효한 경매 생성 요청")
        void validateCreate_Success() {
            // Given
            LocalDateTime startTime = LocalDateTime.now().plusHours(2);
            LocalDateTime endTime = startTime.plusDays(2);

            CreateAuctionRequest request = new CreateAuctionRequest(
                    testProductSizeId,
                    testSellerId,
                    "[새제품] Nike Air Jordan 1",
                    "설명",
                    AuctionCondition.DEADSTOCK,
                    250000L,
                    10000L,
                    400000L,
                    startTime,
                    endTime
            );

            // When & Then
            assertThatNoException()
                    .isThrownBy(() -> auctionValidator.validateCreate(request));
        }

        @Test
        @DisplayName("실패: 즉시구매가가 시작가보다 작음")
        void validateCreate_InvalidBuyNowPrice() {
            // Given
            LocalDateTime startTime = LocalDateTime.now().plusHours(2);
            LocalDateTime endTime = startTime.plusDays(2);

            CreateAuctionRequest request = new CreateAuctionRequest(
                    testProductSizeId,
                    testSellerId,
                    "[새제품] Nike Air Jordan 1",
                    "설명",
                    AuctionCondition.DEADSTOCK,
                    250000L,
                    10000L,
                    200000L, // startPrice보다 작음
                    startTime,
                    endTime
            );

            // When & Then
            assertThatThrownBy(() -> auctionValidator.validateCreate(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("즉시구매가는 시작가보다 커야 합니다");
        }

        @Test
        @DisplayName("실패: 시작 시간이 현재 + 1시간 이전")
        void validateCreate_InvalidStartTime() {
            // Given
            LocalDateTime startTime = LocalDateTime.now().plusMinutes(30); // 30분 후
            LocalDateTime endTime = startTime.plusDays(2);

            CreateAuctionRequest request = new CreateAuctionRequest(
                    testProductSizeId,
                    testSellerId,
                    "[새제품] Nike Air Jordan 1",
                    "설명",
                    AuctionCondition.DEADSTOCK,
                    250000L,
                    10000L,
                    400000L,
                    startTime,
                    endTime
            );

            // When & Then
            assertThatThrownBy(() -> auctionValidator.validateCreate(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("시작 시간은 현재 시간 + 1시간 이후여야 합니다");
        }

        @Test
        @DisplayName("실패: 종료 시간이 시작 시간 + 1시간 이전")
        void validateCreate_InvalidEndTime() {
            // Given
            LocalDateTime startTime = LocalDateTime.now().plusHours(2);
            LocalDateTime endTime = startTime.plusMinutes(30); // 시작 + 30분

            CreateAuctionRequest request = new CreateAuctionRequest(
                    testProductSizeId,
                    testSellerId,
                    "[새제품] Nike Air Jordan 1",
                    "설명",
                    AuctionCondition.DEADSTOCK,
                    250000L,
                    10000L,
                    400000L,
                    startTime,
                    endTime
            );

            // When & Then
            assertThatThrownBy(() -> auctionValidator.validateCreate(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("종료 시간은 시작 시간 + 1시간 이후여야 합니다");
        }

        @Test
        @DisplayName("실패: 경매 기간이 7일 초과")
        void validateCreate_ExceedsMaxPeriod() {
            // Given
            LocalDateTime startTime = LocalDateTime.now().plusHours(2);
            LocalDateTime endTime = startTime.plusDays(8); // 8일 후

            CreateAuctionRequest request = new CreateAuctionRequest(
                    testProductSizeId,
                    testSellerId,
                    "[새제품] Nike Air Jordan 1",
                    "설명",
                    AuctionCondition.DEADSTOCK,
                    250000L,
                    10000L,
                    400000L,
                    startTime,
                    endTime
            );

            // When & Then
            assertThatThrownBy(() -> auctionValidator.validateCreate(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("경매 기간은 최대 7일까지 가능합니다");
        }
    }

    @Nested
    @DisplayName("경매 수정 검증")
    class ValidateUpdateTest {

        private Auction pendingAuction;

        @BeforeEach
        void setUp() {
            pendingAuction = Auction.builder()
                    .productSizeId(testProductSizeId)
                    .sellerId(testSellerId)
                    .auctionTitle("[새제품] Nike Air Jordan 1")
                    .description("설명")
                    .condition(AuctionCondition.DEADSTOCK)
                    .startPrice(250000L)
                    .bidIncrement(10000L)
                    .buyNowPrice(400000L)
                    .startTime(LocalDateTime.now().plusHours(2))
                    .endTime(LocalDateTime.now().plusDays(2))
                    .build();
            pendingAuction.confirmCreation(); // PENDING 상태
        }

        @Test
        @DisplayName("성공: PENDING 상태에서 수정")
        void validateUpdate_PendingStatus_Success() {
            // Given
            UpdateAuctionRequest request = new UpdateAuctionRequest(
                    "[수정] 새로운 제목",
                    "새로운 설명",
                    null,
                    null,
                    null
            );

            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(pendingAuction));

            // When & Then
            assertThatNoException()
                    .isThrownBy(() -> auctionValidator.validateUpdate(testAuctionId, request));
        }

        @Test
        @DisplayName("실패: ACTIVE 상태에서 수정 시도")
        void validateUpdate_ActiveStatus_Fail() {
            // Given
            pendingAuction.start(); // ACTIVE 상태로 전환

            UpdateAuctionRequest request = new UpdateAuctionRequest(
                    "[수정] 새로운 제목",
                    null,
                    null,
                    null,
                    null
            );

            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(pendingAuction));

            // When & Then
            assertThatThrownBy(() -> auctionValidator.validateUpdate(testAuctionId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING 상태에서만 수정할 수 있습니다");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 경매")
        void validateUpdate_AuctionNotFound() {
            // Given
            UpdateAuctionRequest request = new UpdateAuctionRequest(
                    "[수정] 새로운 제목",
                    null,
                    null,
                    null,
                    null
            );

            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> auctionValidator.validateUpdate(testAuctionId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("경매를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("경매 취소 검증")
    class ValidateCancelTest {

        private Auction testAuction;

        @BeforeEach
        void setUp() {
            testAuction = Auction.builder()
                    .productSizeId(testProductSizeId)
                    .sellerId(testSellerId)
                    .auctionTitle("[새제품] Nike Air Jordan 1")
                    .description("설명")
                    .condition(AuctionCondition.DEADSTOCK)
                    .startPrice(250000L)
                    .bidIncrement(10000L)
                    .startTime(LocalDateTime.now().plusHours(2))
                    .endTime(LocalDateTime.now().plusDays(2))
                    .build();
        }

        @Test
        @DisplayName("성공: PENDING 상태 경매 취소")
        void validateCancel_PendingStatus_Success() {
            // Given
            testAuction.confirmCreation(); // PENDING

            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));

            // When & Then
            assertThatNoException()
                    .isThrownBy(() -> auctionValidator.validateCancel(testAuctionId));
        }

        @Test
        @DisplayName("성공: ACTIVE 상태 + 입찰 없음")
        void validateCancel_ActiveWithoutBids_Success() {
            // Given
            testAuction.confirmCreation();
            testAuction.start(); // ACTIVE, totalBidsCount = 0

            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));

            // When & Then
            assertThatNoException()
                    .isThrownBy(() -> auctionValidator.validateCancel(testAuctionId));
        }

        @Test
        @DisplayName("실패: ACTIVE 상태 + 입찰 있음")
        void validateCancel_ActiveWithBids_Fail() {
            // Given
            testAuction.confirmCreation();
            testAuction.start();
            testAuction.updateCurrentPrice(260000L); // 입찰 발생

            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));

            // When & Then
            assertThatThrownBy(() -> auctionValidator.validateCancel(testAuctionId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("입찰이 있는 경매는 취소할 수 없습니다");
        }

        @Test
        @DisplayName("실패: SUCCESS 상태 (취소 불가)")
        void validateCancel_SuccessStatus_Fail() {
            // Given
            testAuction.confirmCreation();
            testAuction.start();
            testAuction.end(true); // SUCCESS

            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));

            // When & Then
            assertThatThrownBy(() -> auctionValidator.validateCancel(testAuctionId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("취소할 수 없는 상태입니다");
        }
    }
}

