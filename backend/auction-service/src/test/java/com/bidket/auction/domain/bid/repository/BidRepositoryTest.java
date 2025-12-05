package com.bidket.auction.domain.bid.repository;

import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.infrastructure.config.JpaAuditingConfig;
import com.bidket.auction.infrastructure.auction.persistence.impl.AuctionJpaRepository;
import com.bidket.auction.infrastructure.bid.persistence.impl.BidJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@DisplayName("BidRepository 테스트")
class BidRepositoryTest {

    @Autowired
    private BidJpaRepository bidRepository;

    @Autowired
    private AuctionJpaRepository auctionJpaRepository;

    @Test
    @DisplayName("입찰을 저장하고 조회할 수 있다")
    void shouldSaveAndFindBid() {
        // Given
        Auction auction = createAndSaveAuction();
        UUID auctionId = auction.getId();
        UUID bidderId = UUID.randomUUID();
        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(350000L)
                .build();

        // When
        Bid saved = bidRepository.save(bid);
        Optional<Bid> found = bidRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualTo(350000L);
    }

    @Test
    @DisplayName("경매 ID로 입찰 목록을 조회할 수 있다")
    void shouldFindBidsByAuctionId() {
        // Given
        Auction auction = createAndSaveAuction();
        UUID auctionId = auction.getId();
        Bid bid1 = createAndSaveBid(auctionId, UUID.randomUUID(), 350000L);
        Bid bid2 = createAndSaveBid(auctionId, UUID.randomUUID(), 360000L);

        // When
        List<Bid> bids = bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId);

        // Then
        assertThat(bids).hasSize(2);
    }

    @Test
    @DisplayName("입찰자 ID로 입찰 목록을 조회할 수 있다")
    void shouldFindBidsByBidderId() {
        // Given
        UUID bidderId = UUID.randomUUID();
        Bid bid1 = createAndSaveBid(createAndSaveAuction().getId(), bidderId, 350000L);
        Bid bid2 = createAndSaveBid(createAndSaveAuction().getId(), bidderId, 360000L);

        // When
        List<Bid> bids = bidRepository.findByBidderIdOrderByCreatedAtDesc(bidderId);

        // Then
        assertThat(bids).hasSize(2);
    }

    @Test
    @DisplayName("경매의 최고가 입찰을 조회할 수 있다")
    void shouldFindHighestBid() {
        // Given
        Auction auction = createAndSaveAuction();
        UUID auctionId = auction.getId();
        Bid bid1 = createAndSaveBid(auctionId, UUID.randomUUID(), 350000L);
        Bid bid2 = createAndSaveBid(auctionId, UUID.randomUUID(), 360000L);
        bid2.markAsHighest();
        bidRepository.save(bid2);

        // When
        Optional<Bid> highest = bidRepository.findFirstByAuctionIdAndBidAmount_HighestTrue(auctionId);

        // Then
        assertThat(highest).isPresent();
        assertThat(highest.get().getAmount()).isEqualTo(360000L);
        assertThat(highest.get().isHighest()).isTrue();
    }

    private Auction createAndSaveAuction() {
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusDays(1);

        Auction auction = Auction.builder()
                .productSizeId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .auctionTitle("Test Auction")
                .description("Test Description")
                .condition(AuctionCondition.NEW)
                .startPrice(10000L)
                .currentPrice(10000L)
                .bidIncrement(1000L)
                .startTime(startTime)
                .endTime(endTime)
                .status(AuctionStatus.CREATING)
                .build();

        return auctionJpaRepository.save(auction);
    }

    private Bid createAndSaveBid(UUID auctionId, UUID bidderId, Long amount) {
        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(amount)
                .build();
        return bidRepository.save(bid);
    }
}

