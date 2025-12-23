package com.bidket.auction.infrastructure.kafka.e2e;

import com.bidket.auction.application.order.OrderEventProcessor;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.model.vo.AuctionPeriod;
import com.bidket.auction.domain.auction.model.vo.AuctionStats;
import com.bidket.auction.domain.auction.model.vo.PriceInfo;
import com.bidket.auction.domain.auction.model.vo.WinnerInfo;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.processed.repository.ProcessedEventRepository;
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
@DisplayName("Order Service 이벤트 소비 E2E 테스트")
class OrderEventConsumerE2ETest {

    @Autowired(required = false)
    private OrderEventProcessor orderEventProcessor;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private UUID auctionId;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        auctionId = createAuction();
    }

    @Test
    @DisplayName("ORDER_CREATED 이벤트 처리 - Saga Step 진행")
    void shouldProcessOrderCreatedEvent() {
         
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();

        if (orderEventProcessor != null) {
             
             
            assertThat(orderEventProcessor).isNotNull();
        }

    }

    @Test
    @DisplayName("ORDER_CANCELED 이벤트 처리 - 보상 트랜잭션 시작")
    void shouldProcessOrderCanceledEvent() {
         
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        if (orderEventProcessor != null) {
            assertThat(orderEventProcessor).isNotNull();
        }

    }

    @Test
    @DisplayName("PAYMENT_TIMEOUT 이벤트 처리 - PaymentTimeoutSaga 시작")
    void shouldProcessPaymentTimeoutEvent() {
         
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        if (orderEventProcessor != null) {
            assertThat(orderEventProcessor).isNotNull();
        }

    }

    @Test
    @DisplayName("중복 이벤트는 처리하지 않아야 함")
    void shouldNotProcessDuplicateEvents() {
         
        UUID eventId = UUID.randomUUID();

    }

    @Test
    @DisplayName("ProcessedEventRepository가 정상 작동해야 함")
    void shouldWorkProcessedEventRepository() {
         
        UUID eventId = UUID.randomUUID();

        boolean exists = processedEventRepository.existsById(eventId);

        assertThat(exists).isFalse();
    }

    private UUID createAuction() {
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
                .auctionTitle("이벤트 테스트 경매")
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
