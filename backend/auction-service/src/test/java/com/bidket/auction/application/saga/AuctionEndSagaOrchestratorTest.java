package com.bidket.auction.application.saga;

import com.bidket.auction.application.compensation.CompensationExecutor;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.model.BidStatus;
import com.bidket.auction.domain.bid.repository.BidRepository;
import com.bidket.auction.domain.saga.model.AuctionEndSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;
import com.bidket.auction.domain.saga.model.SagaStep;
import com.bidket.auction.domain.saga.repository.AuctionEndSagaContextRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import com.bidket.auction.infrastructure.notification.NotificationEventProducer;
import com.bidket.auction.infrastructure.order.OrderEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionEndSagaOrchestrator 단위 테스트")
class AuctionEndSagaOrchestratorTest {

    @Mock
    private AuctionEndSagaContextRepository sagaRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private NotificationEventProducer notificationEventProducer;

    @Mock
    private CompensationExecutor compensationExecutor;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuctionEndSagaOrchestrator sagaOrchestrator;

    private UUID auctionId;
    private UUID winnerId;
    private UUID winningBidId;
    private UUID productSizeId;
    private Auction auction;
    private Bid winningBid;

    @BeforeEach
    void setUp() {
        auctionId = UUID.randomUUID();
        winnerId = UUID.randomUUID();
        winningBidId = UUID.randomUUID();
        productSizeId = UUID.randomUUID();

        auction = Auction.builder()
                .id(auctionId)
                .productSizeId(productSizeId)
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .condition(AuctionCondition.NEW)
                .startPrice(100000L)
                .currentPrice(150000L)
                .bidIncrement(10000L)
                .buyNowPrice(null)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().minusHours(1))
                .originalEndTime(LocalDateTime.now().minusDays(1))
                .extensionCount(0)
                .winnerId(null)
                .winningBidId(null)
                .finalPrice(null)
                .totalBidsCount(5)
                .viewCount(100)
                .status(AuctionStatus.ACTIVE)
                .build();

        winningBid = Bid.builder()
                .id(winningBidId)
                .auctionId(auctionId)
                .bidderId(winnerId)
                .amount(150000L)
                .isHighest(true)
                .status(BidStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("경매 종료 Saga 시작 성공")
    void startAuctionEndSaga_Success() {
        // given
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findHighestBidByAuctionId(auctionId)).thenReturn(Optional.of(winningBid));

        AuctionEndSagaContext savedContext = AuctionEndSagaContext.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .finalPrice(150000L)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(SagaStep.FINALIZE_AUCTION)
                .build();

        when(sagaRepository.save(any(AuctionEndSagaContext.class)))
                .thenAnswer(invocation -> {
                    AuctionEndSagaContext context = invocation.getArgument(0);
                    return AuctionEndSagaContext.builder()
                            .id(savedContext.getId())
                            .auctionId(context.getAuctionId())
                            .winnerId(context.getWinnerId())
                            .winningBidId(context.getWinningBidId())
                            .productSizeId(context.getProductSizeId())
                            .finalPrice(context.getFinalPrice())
                            .status(context.getStatus())
                            .currentStep(context.getCurrentStep())
                            .build();
                });

        // when
        UUID sagaId = sagaOrchestrator.startAuctionEndSaga(auctionId);

        // then
        assertThat(sagaId).isNotNull();
        verify(auctionRepository, atLeastOnce()).findById(auctionId);
        verify(bidRepository).findHighestBidByAuctionId(auctionId);
        verify(sagaRepository, atLeast(2)).save(any(AuctionEndSagaContext.class));
        verify(orderEventProducer, atLeastOnce()).publishCreateOrderRequest(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("경매가 존재하지 않으면 예외 발생")
    void startAuctionEndSaga_AuctionNotFound() {
        // given
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sagaOrchestrator.startAuctionEndSaga(auctionId))
                .isInstanceOf(AuctionDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuctionErrorCode.AUCTION_NOT_FOUND);

        verify(auctionRepository).findById(auctionId);
        verify(bidRepository, never()).findHighestBidByAuctionId(any());
        verify(sagaRepository, never()).save(any());
    }

    @Test
    @DisplayName("낙찰 입찰이 없으면 예외 발생")
    void startAuctionEndSaga_NoBidsFound() {
        // given
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findHighestBidByAuctionId(auctionId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sagaOrchestrator.startAuctionEndSaga(auctionId))
                .isInstanceOf(AuctionDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuctionErrorCode.NO_BIDS_FOUND);

        verify(auctionRepository).findById(auctionId);
        verify(bidRepository).findHighestBidByAuctionId(auctionId);
        verify(sagaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Step 1: CREATE_ORDER 실행 성공")
    void executeCreateOrderStep_Success() {
        // given
        AuctionEndSagaContext sagaContext = AuctionEndSagaContext.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .finalPrice(150000L)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(SagaStep.CREATE_ORDER)
                .correlationId(UUID.randomUUID())
                .build();

        // when
        sagaOrchestrator.executeCreateOrderStep(sagaContext);

        // then
        verify(orderEventProducer).publishCreateOrderRequest(
                any(UUID.class),
                eq(auctionId),
                eq(winnerId),
                eq(productSizeId),
                eq(150000L),
                any(UUID.class)
        );
    }

    @Test
    @DisplayName("Step 2: MARK_WINNING_BID 실행 성공")
    void executeMarkWinningBidStep_Success() {
        // given
        AuctionEndSagaContext sagaContext = AuctionEndSagaContext.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .finalPrice(150000L)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(SagaStep.MARK_WINNING_BID)
                .correlationId(UUID.randomUUID())
                .build();

        when(bidRepository.findById(winningBidId)).thenReturn(Optional.of(winningBid));
        when(bidRepository.save(any(Bid.class))).thenReturn(winningBid);
        when(sagaRepository.save(any(AuctionEndSagaContext.class))).thenReturn(sagaContext);
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        // when
        sagaOrchestrator.executeMarkWinningBidStep(sagaContext);

        // then
        verify(bidRepository).findById(winningBidId);
        verify(bidRepository).save(winningBid);
        verify(sagaRepository, atLeastOnce()).save(sagaContext);
        assertThat(winningBid.getStatus()).isEqualTo(BidStatus.WON);
    }

    @Test
    @DisplayName("Step 3: FINALIZE_AUCTION 실행 성공")
    void executeFinalizeAuctionStep_Success() {
        // given
        AuctionEndSagaContext sagaContext = AuctionEndSagaContext.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .finalPrice(150000L)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(SagaStep.FINALIZE_AUCTION)
                .correlationId(UUID.randomUUID())
                .build();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);
        when(sagaRepository.save(any(AuctionEndSagaContext.class))).thenReturn(sagaContext);
        when(bidRepository.findByAuctionId(auctionId)).thenReturn(java.util.List.of(winningBid));

        // when
        sagaOrchestrator.executeFinalizeAuctionStep(sagaContext);

        // then
        verify(auctionRepository, atLeastOnce()).findById(auctionId);
        verify(auctionRepository, atLeastOnce()).save(auction);
        verify(sagaRepository, atLeastOnce()).save(sagaContext);
    }

    @Test
    @DisplayName("Step 4: PUBLISH_END_EVENT 실행 성공")
    void executePublishEndEventStep_Success() {
        // given
        AuctionEndSagaContext sagaContext = AuctionEndSagaContext.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .finalPrice(150000L)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(SagaStep.PUBLISH_END_EVENT)
                .correlationId(UUID.randomUUID())
                .build();

        when(sagaRepository.save(any(AuctionEndSagaContext.class))).thenReturn(sagaContext);
        when(bidRepository.findByAuctionId(auctionId)).thenReturn(java.util.List.of(winningBid));

        // when
        sagaOrchestrator.executePublishEndEventStep(sagaContext);

        // then
        verify(sagaRepository).save(sagaContext);
        verify(notificationEventProducer).publishWinnerNotification(
                eq(winnerId),
                eq(auctionId),
                eq(150000L),
                any(LocalDateTime.class),
                any(UUID.class)
        );
        verify(bidRepository).findByAuctionId(auctionId);
    }
}
