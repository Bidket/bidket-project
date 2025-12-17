package com.bidket.auction.presentation.bid.api;

import com.bidket.auction.application.bid.dto.request.CreateBidRequest;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("BidController 통합 테스트")
class BidControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BidService bidService;

    @Autowired
    private AuctionRepository auctionRepository;

    private UUID auctionId;
    private UUID sellerId;
    private UUID bidderId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        bidderId = UUID.randomUUID();

        PriceInfo priceInfo = PriceInfo.builder()
                .startPrice(300000L)
                .currentPrice(300000L)
                .bidIncrement(10000L)
                .buyNowPrice(500000L)
                .build();

        AuctionPeriod period = AuctionPeriod.builder()
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .originalEndTime(LocalDateTime.now().plusHours(1))
                .extensionCount(0)
                .build();

        Auction auction = Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(sellerId)
                .auctionTitle("통합 테스트 경매")
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
        auctionId = savedAuction.getId();
    }

    @Test
    @DisplayName("POST /api/v1/bids - 입찰을 등록할 수 있다")
    void shouldCreateBid() throws Exception {
        // Given
        CreateBidRequest request = new CreateBidRequest(auctionId, 350000L);

        // When & Then
        mockMvc.perform(post("/api/v1/bids")
                        .header("X-User-Id", bidderId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("입찰이 성공적으로 등록되었습니다"))
                .andExpect(jsonPath("$.data.amount").value(350000))
                .andExpect(jsonPath("$.data.highest").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("DELETE /api/v1/bids/{bidId} - 입찰을 취소할 수 있다")
    void shouldCancelBid() throws Exception {
        // Given
        Bid bid = bidService.placeBid(auctionId, bidderId, 310000L);
        // 더 높은 입찰로 OUTBID 상태로 만듦
        bidService.placeBid(auctionId, UUID.randomUUID(), 320000L);

        // When & Then
        mockMvc.perform(delete("/api/v1/bids/" + bid.getId())
                        .header("X-User-Id", bidderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("입찰이 취소되었습니다"));
    }

    @Test
    @DisplayName("DELETE /api/v1/bids/{bidId} - 최고가 입찰은 취소할 수 없다")
    void shouldNotCancelHighestBid() throws Exception {
        // Given
        Bid highestBid = bidService.placeBid(auctionId, bidderId, 350000L);

        // When & Then
        mockMvc.perform(delete("/api/v1/bids/" + highestBid.getId())
                        .header("X-User-Id", bidderId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/bids/auction/{auctionId}/buy-now - 즉시 구매할 수 있다")
    void shouldBuyNow() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/bids/auction/" + auctionId + "/buy-now")
                        .header("X-User-Id", bidderId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("즉시 구매가 완료되었습니다"))
                .andExpect(jsonPath("$.data.amount").value(500000))
                .andExpect(jsonPath("$.data.highest").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/bids/auction/{auctionId} - 경매별 입찰 목록을 조회할 수 있다")
    void shouldGetBidsByAuction() throws Exception {
        // Given
        bidService.placeBid(auctionId, UUID.randomUUID(), 310000L);
        bidService.placeBid(auctionId, UUID.randomUUID(), 320000L);
        bidService.placeBid(auctionId, UUID.randomUUID(), 330000L);

        // When & Then
        mockMvc.perform(get("/api/v1/bids/auction/" + auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.bids", hasSize(3)));
    }

    @Test
    @DisplayName("GET /api/v1/bids/my - 내 입찰 목록을 조회할 수 있다")
    void shouldGetMyBids() throws Exception {
        // Given
        bidService.placeBid(auctionId, bidderId, 310000L);
        bidService.placeBid(auctionId, bidderId, 320000L);

        // When & Then
        mockMvc.perform(get("/api/v1/bids/my")
                        .header("X-User-Id", bidderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.bids", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/v1/bids/{bidId} - 입찰 상세를 조회할 수 있다")
    void shouldGetBid() throws Exception {
        // Given
        Bid bid = bidService.placeBid(auctionId, bidderId, 350000L);

        // When & Then
        mockMvc.perform(get("/api/v1/bids/" + bid.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(bid.getId().toString()))
                .andExpect(jsonPath("$.data.amount").value(350000))
                .andExpect(jsonPath("$.data.highest").value(true));
    }
}

