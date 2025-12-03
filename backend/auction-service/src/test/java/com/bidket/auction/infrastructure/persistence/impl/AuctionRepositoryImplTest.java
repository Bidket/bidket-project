package com.bidket.auction.infrastructure.persistence.impl;

import com.bidket.auction.domain.model.Auction;
import com.bidket.auction.domain.model.AuctionCondition;
import com.bidket.auction.domain.model.AuctionStatus;
import com.bidket.auction.domain.repository.AuctionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("AuctionRepository 통합 테스트")
class AuctionRepositoryImplTest {

    @Autowired
    private AuctionJpaRepository jpaRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    private UUID testProductSizeId;
    private UUID testSellerId;
    private Auction testAuction;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();

        testProductSizeId = UUID.randomUUID();
        testSellerId = UUID.randomUUID();

        testAuction = Auction.builder()
                .productSizeId(testProductSizeId)
                .sellerId(testSellerId)
                .auctionTitle("[새제품] Nike Air Jordan 1")
                .description("새 제품입니다")
                .condition(AuctionCondition.DEADSTOCK)
                .startPrice(250000L)
                .bidIncrement(10000L)
                .buyNowPrice(400000L)
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusDays(2))
                .build();
    }

    @Nested
    @DisplayName("경매 저장 및 조회")
    class SaveAndFindTest {

        @Test
        @DisplayName("성공: 경매 저장")
        void save_Success() {
            // When
            Auction saved = auctionRepository.save(testAuction);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getAuctionTitle()).isEqualTo("[새제품] Nike Air Jordan 1");
            assertThat(saved.getStatus()).isEqualTo(AuctionStatus.CREATING);
            assertThat(saved.getCurrentPrice()).isEqualTo(250000L);
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("성공: ID로 경매 조회")
        void findById_Success() {
            // Given
            Auction saved = auctionRepository.save(testAuction);

            // When
            Optional<Auction> found = auctionRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
            assertThat(found.get().getAuctionTitle()).isEqualTo("[새제품] Nike Air Jordan 1");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID 조회")
        void findById_NotFound() {
            // When
            Optional<Auction> found = auctionRepository.findById(UUID.randomUUID());

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("판매자별 조회")
    class FindBySellerTest {

        @Test
        @DisplayName("성공: 판매자 ID로 경매 목록 조회")
        void findBySellerId_Success() {
            // Given
            auctionRepository.save(testAuction);

            Auction anotherAuction = Auction.builder()
                    .productSizeId(UUID.randomUUID())
                    .sellerId(testSellerId) // 동일 판매자
                    .auctionTitle("[중고] Adidas Yeezy")
                    .condition(AuctionCondition.USED)
                    .startPrice(150000L)
                    .bidIncrement(5000L)
                    .startTime(LocalDateTime.now().plusHours(3))
                    .endTime(LocalDateTime.now().plusDays(3))
                    .build();
            auctionRepository.save(anotherAuction);

            // When
            List<Auction> auctions = auctionRepository.findBySellerId(testSellerId);

            // Then
            assertThat(auctions).hasSize(2);
            assertThat(auctions).extracting("sellerId")
                    .containsOnly(testSellerId);
        }

        @Test
        @DisplayName("빈 결과: 해당 판매자의 경매가 없는 경우")
        void findBySellerId_Empty() {
            // When
            List<Auction> auctions = auctionRepository.findBySellerId(UUID.randomUUID());

            // Then
            assertThat(auctions).isEmpty();
        }
    }

    @Nested
    @DisplayName("상태별 조회")
    class FindByStatusTest {

        @Test
        @DisplayName("성공: 상태별 경매 목록 조회")
        void findByStatus_Success() {
            // Given
            testAuction.confirmCreation();
            testAuction.start();
            auctionRepository.save(testAuction);

            Auction anotherAuction = Auction.builder()
                    .productSizeId(UUID.randomUUID())
                    .sellerId(UUID.randomUUID())
                    .auctionTitle("[새제품] Nike Air Max")
                    .condition(AuctionCondition.NEW)
                    .startPrice(200000L)
                    .bidIncrement(10000L)
                    .startTime(LocalDateTime.now().plusHours(2))
                    .endTime(LocalDateTime.now().plusDays(2))
                    .build();
            auctionRepository.save(anotherAuction); // CREATING 상태

            // When
            List<Auction> activeAuctions = auctionRepository.findByStatus(AuctionStatus.ACTIVE);
            List<Auction> creatingAuctions = auctionRepository.findByStatus(AuctionStatus.CREATING);

            // Then
            assertThat(activeAuctions).hasSize(1);
            assertThat(activeAuctions.get(0).getStatus()).isEqualTo(AuctionStatus.ACTIVE);

            assertThat(creatingAuctions).hasSize(1);
            assertThat(creatingAuctions.get(0).getStatus()).isEqualTo(AuctionStatus.CREATING);
        }
    }

    @Nested
    @DisplayName("시간 기반 조회")
    class TimeBasedQueryTest {

        @Test
        @DisplayName("성공: 종료 시간이 도래한 ACTIVE 경매 조회")
        void findActiveAuctionsEndingBefore_Success() {
            // Given
            LocalDateTime pastEndTime = LocalDateTime.now().minusHours(1);
            Auction expiredAuction = Auction.builder()
                    .productSizeId(UUID.randomUUID())
                    .sellerId(UUID.randomUUID())
                    .auctionTitle("[만료] 경매")
                    .condition(AuctionCondition.NEW)
                    .startPrice(100000L)
                    .bidIncrement(5000L)
                    .startTime(pastEndTime.minusDays(1))
                    .endTime(pastEndTime)
                    .build();
            expiredAuction.confirmCreation();
            expiredAuction.start();
            auctionRepository.save(expiredAuction);

            // 아직 종료 안 된 경매
            testAuction.confirmCreation();
            testAuction.start();
            auctionRepository.save(testAuction);

            // When
            List<Auction> expiredAuctions = auctionRepository
                    .findActiveAuctionsEndingBefore(LocalDateTime.now());

            // Then
            assertThat(expiredAuctions).hasSize(1);
            assertThat(expiredAuctions.get(0).getAuctionTitle()).isEqualTo("[만료] 경매");
        }

        @Test
        @DisplayName("성공: 시작 시간이 도래한 PENDING 경매 조회")
        void findPendingAuctionsStartingBefore_Success() {
            // Given
            LocalDateTime pastStartTime = LocalDateTime.now().minusHours(1);
            Auction shouldStartAuction = Auction.builder()
                    .productSizeId(UUID.randomUUID())
                    .sellerId(UUID.randomUUID())
                    .auctionTitle("[시작 예정] 경매")
                    .condition(AuctionCondition.NEW)
                    .startPrice(100000L)
                    .bidIncrement(5000L)
                    .startTime(pastStartTime)
                    .endTime(LocalDateTime.now().plusDays(1))
                    .build();
            shouldStartAuction.confirmCreation();
            auctionRepository.save(shouldStartAuction);

            // 아직 시작 안 된 경매
            testAuction.confirmCreation();
            auctionRepository.save(testAuction);

            // When
            List<Auction> pendingAuctions = auctionRepository
                    .findPendingAuctionsStartingBefore(LocalDateTime.now());

            // Then
            assertThat(pendingAuctions).hasSize(1);
            assertThat(pendingAuctions.get(0).getAuctionTitle()).isEqualTo("[시작 예정] 경매");
        }
    }

    @Nested
    @DisplayName("경매 삭제")
    class DeleteTest {

        @Test
        @DisplayName("성공: 경매 삭제")
        void delete_Success() {
            // Given
            Auction saved = auctionRepository.save(testAuction);

            // When
            auctionRepository.delete(saved);

            // Then
            Optional<Auction> found = auctionRepository.findById(saved.getId());
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("존재 여부 확인")
    class ExistsTest {

        @Test
        @DisplayName("성공: 경매 존재 여부 확인")
        void existsById_Success() {
            // Given
            Auction saved = auctionRepository.save(testAuction);

            // When
            boolean exists = auctionRepository.existsById(saved.getId());
            boolean notExists = auctionRepository.existsById(UUID.randomUUID());

            // Then
            assertThat(exists).isTrue();
            assertThat(notExists).isFalse();
        }
    }

    @Nested
    @DisplayName("Optimistic Lock 테스트")
    class OptimisticLockTest {

        @Test
        @DisplayName("성공: 버전 증가 확인")
        void optimisticLock_VersionIncrement() {
            // Given
            Auction saved = auctionRepository.save(testAuction);
            jpaRepository.flush(); 
            Long initialVersion = saved.getVersion();

            // When
            saved.confirmCreation();
            Auction updated = auctionRepository.save(saved);
            jpaRepository.flush(); 

            // Then
            assertThat(updated.getVersion()).isGreaterThan(initialVersion);
        }
    }
}

