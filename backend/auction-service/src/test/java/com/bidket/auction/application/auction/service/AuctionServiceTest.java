package com.bidket.auction.application.auction.service;

import com.bidket.auction.application.auction.dto.request.CreateAuctionRequest;
import com.bidket.auction.application.auction.dto.request.UpdateAuctionRequest;
import com.bidket.auction.application.auction.dto.response.AuctionResponse;
import com.bidket.auction.application.outbox.service.OutboxService;
import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.model.vo.AuctionPeriod;
import com.bidket.auction.domain.auction.model.vo.AuctionStats;
import com.bidket.auction.domain.auction.model.vo.PriceInfo;
import com.bidket.auction.domain.auction.model.vo.WinnerInfo;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.auction.service.AuctionValidator;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import com.bidket.auction.infrastructure.redis.ViewCountCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionService 단위 테스트")
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionValidator auctionValidator;

    @Mock
    private ViewCountCacheService viewCountCacheService;

    @Mock
    private OutboxService outboxService;

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

            CreateAuctionRequest request = new CreateAuctionRequest(
                    testProductSizeId,
                    "[새제품] Nike Air Jordan 1",
                    "새 제품입니다",
                    AuctionCondition.DEADSTOCK,
                    250000L,
                    10000L,
                    400000L,
                    LocalDateTime.now().plusHours(2),
                    LocalDateTime.now().plusDays(2)
            );

            Auction savedAuction = Auction.builder()
                    .id(testAuctionId)
                    .productSizeId(testProductSizeId)
                    .sellerId(testSellerId)
                    .auctionTitle("[새제품] Nike Air Jordan 1")
                    .description("새 제품입니다")
                    .condition(AuctionCondition.DEADSTOCK)
                    .priceInfo(PriceInfo.builder()
                            .startPrice(250000L)
                            .currentPrice(250000L)
                            .bidIncrement(10000L)
                            .buyNowPrice(400000L)
                            .build())
                    .period(AuctionPeriod.builder()
                            .startTime(LocalDateTime.now().plusHours(2))
                            .endTime(LocalDateTime.now().plusDays(2))
                            .originalEndTime(LocalDateTime.now().plusDays(2))
                            .extensionCount(0)
                            .build())
                    .stats(AuctionStats.createDefault())
                    .winnerInfo(WinnerInfo.empty())
                    .status(AuctionStatus.CREATING)
                    .build();

            given(auctionRepository.save(any(Auction.class)))
                    .willReturn(savedAuction);

            AuctionOutbox mockOutbox = AuctionOutbox.pending(
                    "AUCTION", testAuctionId, "AUCTION_CREATED", "{}", UUID.randomUUID()
            );
            given(outboxService.saveAuctionEvent(anyString(), any(UUID.class), any(Map.class), any(UUID.class)))
                    .willReturn(mockOutbox);

            AuctionResponse response = auctionService.createAuction(testSellerId, request);

            assertThat(response).isNotNull();
            assertThat(response.auctionTitle()).isEqualTo("[새제품] Nike Air Jordan 1");
            assertThat(response.status()).isEqualTo(AuctionStatus.CREATING);
            assertThat(response.startPrice()).isEqualTo(250000L);
            assertThat(response.currentPrice()).isEqualTo(250000L);

            verify(auctionValidator).validateCreate(request);
            verify(auctionRepository).save(any(Auction.class));
            verify(outboxService).saveAuctionEvent(
                    eq("AUCTION_CREATED"),
                    any(UUID.class),
                    any(Map.class),
                    any(UUID.class)
            );
        }

        @Test
        @DisplayName("실패: 검증 실패 시 예외 발생")
        void createAuction_ValidationFailed() {

            CreateAuctionRequest request = new CreateAuctionRequest(
                    testProductSizeId,
                    "짧음",
                    null,
                    AuctionCondition.NEW,
                    250000L,
                    10000L,
                    null,
                    LocalDateTime.now().plusHours(2),
                    LocalDateTime.now().plusDays(2)
            );

            doThrow(new AuctionDomainException(AuctionErrorCode.INVALID_AUCTION_STATUS))
                    .when(auctionValidator).validateCreate(request);

            assertThatThrownBy(() -> auctionService.createAuction(testSellerId, request))
                    .isInstanceOf(AuctionDomainException.class);

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
             
            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));
            given(viewCountCacheService.getViewCount(any(UUID.class), any(Integer.class)))
                    .willReturn(0);

            AuctionResponse response = auctionService.getAuction(testAuctionId);

            assertThat(response).isNotNull();
            assertThat(response.auctionTitle()).isEqualTo("[새제품] Nike Air Jordan 1");

            verify(auctionRepository).findById(testAuctionId);
            verify(viewCountCacheService).incrementViewCountAsync(testAuctionId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 경매")
        void getAuction_NotFound() {
             
            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> auctionService.getAuction(testAuctionId))
                    .isInstanceOf(AuctionDomainException.class);

            verify(auctionRepository).findById(testAuctionId);
        }
    }

    @Nested
    @DisplayName("경매 목록 조회 테스트")
    class GetAuctionsTest {

        @Test
        @DisplayName("성공: 판매자별 경매 목록 조회")
        void getAuctionsBySeller_Success() {
             
            List<Auction> auctions = List.of(testAuction);
            given(auctionRepository.findBySellerId(testSellerId))
                    .willReturn(auctions);

            List<AuctionResponse> responses = auctionService.getAuctionsBySeller(testSellerId);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).sellerId()).isEqualTo(testSellerId);

            verify(auctionRepository).findBySellerId(testSellerId);
        }

        @Test
        @DisplayName("성공: 상태별 경매 목록 페이징 조회")
        void getAuctionsByStatusWithPaging_Success() {

            testAuction.confirmCreation();
            testAuction.start();

            List<Auction> auctions = List.of(testAuction);
            Pageable pageable = PageRequest.of(0, 20);
            Page<Auction> auctionPage = new PageImpl<>(auctions, pageable, 1);

            given(auctionRepository.findByStatus(AuctionStatus.ACTIVE, pageable))
                    .willReturn(auctionPage);

            Page<AuctionResponse> responses = auctionService
                    .getAuctionsByStatusWithPaging(AuctionStatus.ACTIVE, pageable);

            assertThat(responses).isNotNull();
            assertThat(responses.getContent()).hasSize(1);
            assertThat(responses.getTotalElements()).isEqualTo(1);
            assertThat(responses.getTotalPages()).isEqualTo(1);
            assertThat(responses.getContent().get(0).status()).isEqualTo(AuctionStatus.ACTIVE);

            verify(auctionRepository).findByStatus(AuctionStatus.ACTIVE, pageable);
        }

        @Test
        @DisplayName("성공: 페이징으로 빈 결과 조회")
        void getAuctionsByStatusWithPaging_EmptyResult() {

            Pageable pageable = PageRequest.of(0, 20);
            Page<Auction> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(auctionRepository.findByStatus(AuctionStatus.ACTIVE, pageable))
                    .willReturn(emptyPage);

            Page<AuctionResponse> responses = auctionService
                    .getAuctionsByStatusWithPaging(AuctionStatus.ACTIVE, pageable);

            assertThat(responses).isNotNull();
            assertThat(responses.getContent()).isEmpty();
            assertThat(responses.getTotalElements()).isEqualTo(0);
            assertThat(responses.getTotalPages()).isEqualTo(0);

            verify(auctionRepository).findByStatus(AuctionStatus.ACTIVE, pageable);
        }
    }

    @Nested
    @DisplayName("경매 수정 테스트")
    class UpdateAuctionTest {

        @Test
        @DisplayName("성공: 유효한 수정 요청")
        void updateAuction_Success() {
             
            testAuction.confirmCreation();  

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

            AuctionResponse response = auctionService.updateAuction(testSellerId, testAuctionId, request);

            assertThat(response).isNotNull();

            verify(auctionValidator).validateUpdate(testAuctionId, request);
            verify(auctionRepository).findById(testAuctionId);
            verify(auctionRepository).save(testAuction);
        }

        @Test
        @DisplayName("실패: 경매를 찾을 수 없음")
        void updateAuction_NotFound() {
             
            UpdateAuctionRequest request = new UpdateAuctionRequest(
                    "[수정] 새로운 제목",
                    null,
                    null,
                    null,
                    null
            );

            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> auctionService.updateAuction(testSellerId, testAuctionId, request))
                    .isInstanceOf(AuctionDomainException.class);
        }
    }

    @Nested
    @DisplayName("경매 취소 테스트")
    class CancelAuctionTest {

        @Test
        @DisplayName("성공: PENDING 상태 경매 취소")
        void cancelAuction_PendingStatus_Success() {
             
            testAuction.confirmCreation();  

            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));

            auctionService.cancelAuction(testSellerId, testAuctionId);

            assertThat(testAuction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);

            verify(auctionValidator).validateCancel(testAuctionId);
            verify(auctionRepository).findById(testAuctionId);
            verify(auctionRepository).save(testAuction);
        }

        @Test
        @DisplayName("실패: 입찰이 있는 경매 취소 시도")
        void cancelAuction_WithBids_Fail() {

            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));

            doThrow(new AuctionDomainException(AuctionErrorCode.CANNOT_CANCEL_WITH_BIDS))
                    .when(auctionValidator).validateCancel(testAuctionId);

            assertThatThrownBy(() -> auctionService.cancelAuction(testSellerId, testAuctionId))
                    .isInstanceOf(AuctionDomainException.class);

            verify(auctionValidator).validateCancel(testAuctionId);
        }
    }

    @Nested
    @DisplayName("경매 확정 테스트")
    class ConfirmAuctionTest {

        @Test
        @DisplayName("성공: CREATING -> PENDING 상태 전환")
        void confirmAuctionCreation_Success() {
             
            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));

            auctionService.confirmAuctionCreation(testSellerId, testAuctionId);

            assertThat(testAuction.getStatus()).isEqualTo(AuctionStatus.PENDING);

            verify(auctionRepository).findById(testAuctionId);
            verify(auctionRepository).save(testAuction);
        }

        @Test
        @DisplayName("실패: CREATING 상태가 아닌 경우")
        void confirmAuctionCreation_InvalidStatus() {
             
            testAuction.confirmCreation();  
            
            given(auctionRepository.findById(testAuctionId))
                    .willReturn(Optional.of(testAuction));

            assertThatThrownBy(() -> auctionService.confirmAuctionCreation(testSellerId, testAuctionId))
                    .isInstanceOf(AuctionDomainException.class)
                    .extracting(e -> ((AuctionDomainException) e).getErrorCode())
                    .isEqualTo(AuctionErrorCode.INVALID_AUCTION_STATUS);

            verify(auctionRepository).findById(testAuctionId);
        }
    }
}
