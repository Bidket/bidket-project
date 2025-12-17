package com.bidket.auction.application.auction.scheduler;

import com.bidket.auction.application.saga.AuctionEndSagaOrchestrator;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.infrastructure.redis.ViewCountCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    //private final ViewCountCacheService viewCountCacheService;

    @Autowired(required = false)
    private AuctionEndSagaOrchestrator sagaOrchestrator;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void startPendingAuctions() {
        LocalDateTime now = LocalDateTime.now();

        List<Auction> pendingAuctions = auctionRepository
                .findPendingAuctionsStartingBefore(now);

        if (pendingAuctions.isEmpty()) {
            return;
        }

        List<Auction> activeAuctions = auctionRepository.findByStatus(AuctionStatus.ACTIVE);
        Set<UUID> productSizeIdsWithActive = activeAuctions.stream()
                .map(Auction::getProductSizeId)
                .collect(toSet());

        Map<UUID, List<Auction>> pendingByProductSize = pendingAuctions.stream()
                .collect(groupingBy(Auction::getProductSizeId));

        int startedCount = 0;

        for (Map.Entry<UUID, List<Auction>> entry : pendingByProductSize.entrySet()) {
            UUID productSizeId = entry.getKey();

            if (productSizeIdsWithActive.contains(productSizeId)) {
                log.debug("productSizeId={} 에 대해 이미 ACTIVE 경매가 있어 시작하지 않음", productSizeId);
                continue;
            }

            List<Auction> auctionsForSize = entry.getValue();
            if (auctionsForSize.isEmpty()) {
                continue;
            }

            Auction auctionToStart = auctionsForSize.get(0);

            try {
                auctionToStart.start();
                auctionRepository.save(auctionToStart);
                startedCount++;

                log.info("경매 시작: {} ({}) - productSizeId={}",
                        auctionToStart.getId(),
                        auctionToStart.getAuctionTitle(),
                        productSizeId);

            } catch (Exception e) {
                log.error("경매 시작 실패: {}", auctionToStart.getId(), e);
            }
        }

        if (startedCount > 0) {
            log.info("경매 자동 시작 완료: {} 건", startedCount);
        }
    }

    /**
     * 경매 자동 종료 Scheduler
     * SAGA-001: 낙찰자가 있으면 경매 종료 Saga 시작, 없으면 EXPIRED 처리
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void endActiveAuctions() {
        LocalDateTime now = LocalDateTime.now();

        List<Auction> activeAuctions = auctionRepository
                .findActiveAuctionsEndingBefore(now);

        if (activeAuctions.isEmpty()) {
            return;
        }

        log.info("경매 자동 종료 실행: {} 건", activeAuctions.size());

        for (Auction auction : activeAuctions) {
            try {
                boolean hasBids = auction.getStats().getTotalBidsCount() > 0;

                if (hasBids && sagaOrchestrator != null) {
                    // 낙찰자 있음 -> Saga 시작 (주문 생성 -> 낙찰 확정)
                    UUID sagaId = sagaOrchestrator.startAuctionEndSaga(auction.getId());
                    log.info("경매 종료 Saga 시작: auctionId={}, sagaId={}", auction.getId(), sagaId);

                } else {
                    // 낙찰자 없음 -> EXPIRED 처리
                    auction.end(false);
                    auctionRepository.save(auction);
                    log.info("경매 종료: {} ({}) - 상태: EXPIRED (입찰 없음)",
                            auction.getId(), auction.getAuctionTitle());
                }

            } catch (Exception e) {
                log.error("경매 종료 실패: {}", auction.getId(), e);
            }
        }

        log.info("경매 자동 종료 완료: {} 건", activeAuctions.size());
    }

    // @Scheduled(fixedDelay = 300000)
    // @Transactional
    // public void syncViewCounts() {
    //     log.info("조회수 캐시 동기화 시작");

    //     try {
    //         int syncedCount = viewCountCacheService.syncViewCountsToDatabase();

    //         if (syncedCount > 0) {
    //             log.info("조회수 캐시 동기화 완료: {} 건", syncedCount);
    //         } else {
    //             log.debug("동기화할 조회수 없음");
    //         }

    //         ViewCountCacheService.CacheStats stats = viewCountCacheService.getCacheStats();
    //         log.debug("현재 캐시 상태: {}", stats);

    //     } catch (Exception e) {
    //         log.error("조회수 캐시 동기화 중 오류 발생", e);
    //     }
    // }
}


