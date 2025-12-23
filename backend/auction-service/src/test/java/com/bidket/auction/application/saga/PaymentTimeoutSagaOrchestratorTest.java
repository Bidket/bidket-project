package com.bidket.auction.application.saga;

import com.bidket.auction.application.compensation.CompensationExecutor;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.model.BidStatus;
import com.bidket.auction.domain.bid.repository.BidRepository;
import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaContext;
import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaStep;
import com.bidket.auction.domain.saga.model.SagaStatus;
import com.bidket.auction.domain.saga.repository.PaymentTimeoutSagaContextRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import com.bidket.auction.infrastructure.notification.NotificationEventProducer;
import com.bidket.auction.infrastructure.order.OrderEventProducer;
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
@DisplayName("PaymentTimeoutSagaOrchestrator 테스트")
class PaymentTimeoutSagaOrchestratorTest {

    @Mock
    private PaymentTimeoutSagaContextRepository sagaRepository;

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

    @InjectMocks
    private PaymentTimeoutSagaOrchestrator sagaOrchestrator;

    private UUID auctionId;
    private UUID orderId;
    private UUID winnerId;
    private UUID winningBidId;
    private UUID productSizeId;
    private Auction auction;
    private Bid winningBid;

    @BeforeEach
    void setUp() {
        auctionId = UUID.randomUUID();
        orderId = UUID.randomUUID();
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
                .currentPrice(110000L)
                .bidIncrement(10000L)
                .buyNowPrice(null)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().minusHours(1))
                .originalEndTime(LocalDateTime.now().minusDays(1))
                .extensionCount(0)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .finalPrice(110000L)
                .totalBidsCount(5)
                .viewCount(100)
                .status(AuctionStatus.SUCCESS)
                .build();

        winningBid = Bid.builder()
                .id(winningBidId)
                .auctionId(auctionId)
                .bidderId(winnerId)
                .amount(110000L)
                .isHighest(true)
                .status(BidStatus.WON)
                .build();
    }

    @Test
    @DisplayName("결제 타임아웃 Saga 시작 성공")
    void startPaymentTimeoutSaga_Success() {
         
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(sagaRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(bidRepository.findById(winningBidId)).thenReturn(Optional.of(winningBid));

        PaymentTimeoutSagaContext savedContext = PaymentTimeoutSagaContext.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .orderId(orderId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(PaymentTimeoutSagaStep.PUBLISH_REOPEN_EVENT)
                .build();

        when(sagaRepository.save(any(PaymentTimeoutSagaContext.class)))
                .thenAnswer(invocation -> {
                    PaymentTimeoutSagaContext context = invocation.getArgument(0);
                    return PaymentTimeoutSagaContext.builder()
                            .id(savedContext.getId())
                            .auctionId(context.getAuctionId())
                            .orderId(context.getOrderId())
                            .winnerId(context.getWinnerId())
                            .winningBidId(context.getWinningBidId())
                            .productSizeId(context.getProductSizeId())
                            .status(context.getStatus())
                            .currentStep(context.getCurrentStep())
                            .build();
                });

        UUID sagaId = sagaOrchestrator.startPaymentTimeoutSaga(auctionId, orderId);

        assertThat(sagaId).isNotNull();
        verify(auctionRepository, atLeastOnce()).findById(auctionId);
        verify(sagaRepository).findByOrderId(orderId);
        verify(bidRepository, atLeastOnce()).findById(winningBidId);
         
        verify(sagaRepository, atLeast(2)).save(any(PaymentTimeoutSagaContext.class));
        verify(auctionRepository, atLeastOnce()).save(any(Auction.class));
        verify(bidRepository, atLeastOnce()).save(any(Bid.class));
    }

    @Test
    @DisplayName("경매가 존재하지 않으면 예외 발생")
    void startPaymentTimeoutSaga_AuctionNotFound() {
         
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sagaOrchestrator.startPaymentTimeoutSaga(auctionId, orderId))
                .isInstanceOf(AuctionDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuctionErrorCode.AUCTION_NOT_FOUND);

        verify(auctionRepository).findById(auctionId);
        verify(sagaRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 진행 중인 Saga가 있으면 예외 발생 (Idempotency)")
    void startPaymentTimeoutSaga_SagaAlreadyExists() {
         
        PaymentTimeoutSagaContext existingSaga = PaymentTimeoutSagaContext.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .orderId(orderId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(PaymentTimeoutSagaStep.REOPEN_AUCTION)
                .build();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(sagaRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingSaga));

        assertThatThrownBy(() -> sagaOrchestrator.startPaymentTimeoutSaga(auctionId, orderId))
                .isInstanceOf(AuctionDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuctionErrorCode.SAGA_ALREADY_EXISTS);

        verify(auctionRepository).findById(auctionId);
        verify(sagaRepository).findByOrderId(orderId);
        verify(sagaRepository, never()).save(any());
    }

    @Test
    @DisplayName("낙찰 입찰이 없으면 예외 발생")
    void startPaymentTimeoutSaga_NoBidsFound() {
         
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(sagaRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(bidRepository.findById(winningBidId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sagaOrchestrator.startPaymentTimeoutSaga(auctionId, orderId))
                .isInstanceOf(AuctionDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuctionErrorCode.BID_NOT_FOUND);

        verify(bidRepository).findById(winningBidId);
    }

    @Test
    @DisplayName("Step 1: 경매 재오픈 성공")
    void executeReopenAuctionStep_Success() {
         
        PaymentTimeoutSagaContext context = PaymentTimeoutSagaContext.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .orderId(orderId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(PaymentTimeoutSagaStep.REOPEN_AUCTION)
                .build();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(sagaRepository.save(any(PaymentTimeoutSagaContext.class))).thenReturn(context);
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        sagaOrchestrator.executeReopenAuctionStep(context);

        verify(auctionRepository).findById(auctionId);
        verify(auctionRepository).save(any(Auction.class));
        verify(sagaRepository).save(any(PaymentTimeoutSagaContext.class));
    }

    @Test
    @DisplayName("Step 2: 입찰 상태 복원 성공")
    void executeRevertBidStatusStep_Success() {
         
        PaymentTimeoutSagaContext context = PaymentTimeoutSagaContext.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .orderId(orderId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(PaymentTimeoutSagaStep.REVERT_BID_STATUS)
                .build();

        when(bidRepository.findById(winningBidId)).thenReturn(Optional.of(winningBid));
        when(sagaRepository.save(any(PaymentTimeoutSagaContext.class))).thenReturn(context);
        when(bidRepository.save(any(Bid.class))).thenReturn(winningBid);

        sagaOrchestrator.executeRevertBidStatusStep(context);

        verify(bidRepository).findById(winningBidId);
        verify(bidRepository).save(any(Bid.class));
        verify(sagaRepository).save(any(PaymentTimeoutSagaContext.class));
    }

    @Test
    @DisplayName("Step 3: 주문 취소 성공 (MVP - 로깅만)")
    void executeCancelOrderStep_Success() {
         
        PaymentTimeoutSagaContext context = PaymentTimeoutSagaContext.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .orderId(orderId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(PaymentTimeoutSagaStep.CANCEL_ORDER)
                .build();

        when(sagaRepository.save(any(PaymentTimeoutSagaContext.class))).thenReturn(context);

        sagaOrchestrator.executeCancelOrderStep(context);

        verify(sagaRepository).save(any(PaymentTimeoutSagaContext.class));
    }

    @Test
    @DisplayName("Step 5: AUCTION_REOPENED 이벤트 발행 성공")
    void executePublishReopenEventStep_Success() {
         
        PaymentTimeoutSagaContext context = PaymentTimeoutSagaContext.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .orderId(orderId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(PaymentTimeoutSagaStep.PUBLISH_REOPEN_EVENT)
                .build();

        when(sagaRepository.save(any(PaymentTimeoutSagaContext.class))).thenReturn(context);
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        sagaOrchestrator.executePublishReopenEventStep(context);

        verify(sagaRepository).save(any(PaymentTimeoutSagaContext.class));
        assertThat(context.getStatus()).isEqualTo(SagaStatus.COMPLETED);
    }

    @Test
    @DisplayName("보상 트랜잭션 실행 성공")
    void compensate_Success() {
         
        UUID sagaId = UUID.randomUUID();
        PaymentTimeoutSagaContext context = PaymentTimeoutSagaContext.builder()
                .id(sagaId)
                .auctionId(auctionId)
                .orderId(orderId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(PaymentTimeoutSagaStep.REOPEN_AUCTION)
                .build();

        when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(context));
        when(sagaRepository.save(any(PaymentTimeoutSagaContext.class))).thenReturn(context);
        doNothing().when(compensationExecutor).executeCompensations(any(), any());

        sagaOrchestrator.compensate(sagaId, "Test failure");

        verify(sagaRepository).findById(sagaId);
        verify(compensationExecutor).executeCompensations(sagaId, "Test failure");
        verify(sagaRepository, atLeast(2)).save(any(PaymentTimeoutSagaContext.class));
    }

    @Test
    @DisplayName("Saga 실행 중 예외 발생 시 보상 트랜잭션 자동 실행")
    void startPaymentTimeoutSaga_FailureTriggersCompensation() {
         
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(sagaRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(bidRepository.findById(winningBidId)).thenReturn(Optional.of(winningBid));

        UUID sagaId = UUID.randomUUID();

        when(sagaRepository.save(any(PaymentTimeoutSagaContext.class)))
                .thenAnswer(invocation -> {
                    PaymentTimeoutSagaContext context = invocation.getArgument(0);
                    return PaymentTimeoutSagaContext.builder()
                            .id(context.getId() != null ? context.getId() : sagaId)
                            .auctionId(context.getAuctionId())
                            .orderId(context.getOrderId())
                            .winnerId(context.getWinnerId())
                            .winningBidId(context.getWinningBidId())
                            .productSizeId(context.getProductSizeId())
                            .status(context.getStatus())
                            .currentStep(context.getCurrentStep())
                            .build();
                });

        when(sagaRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    return Optional.of(PaymentTimeoutSagaContext.builder()
                            .id(sagaId)
                            .auctionId(auctionId)
                            .orderId(orderId)
                            .winnerId(winnerId)
                            .winningBidId(winningBidId)
                            .productSizeId(productSizeId)
                            .status(SagaStatus.IN_PROGRESS)
                            .currentStep(PaymentTimeoutSagaStep.REOPEN_AUCTION)
                            .build());
                });

        when(auctionRepository.save(any(Auction.class)))
                .thenThrow(new RuntimeException("Database error"));

        doNothing().when(compensationExecutor).executeCompensations(any(), any());

        try {
            sagaOrchestrator.startPaymentTimeoutSaga(auctionId, orderId);
        } catch (Exception e) {
             
        }

        verify(compensationExecutor).executeCompensations(eq(sagaId), anyString());
    }
}
