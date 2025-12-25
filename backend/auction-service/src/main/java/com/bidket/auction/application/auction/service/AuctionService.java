package com.bidket.auction.application.auction.service;

import com.bidket.auction.application.auction.dto.request.CreateAuctionRequest;
import com.bidket.auction.application.auction.dto.request.UpdateAuctionRequest;
import com.bidket.auction.application.auction.dto.response.AuctionResponse;
import com.bidket.auction.application.outbox.service.OutboxService;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.auction.service.AuctionValidator;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import com.bidket.auction.infrastructure.redis.ViewCountCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionValidator auctionValidator;
    private final ViewCountCacheService viewCountCacheService;
    private final OutboxService outboxService;

    @Transactional
    @CacheEvict(value = {"auctions", "auctionsByStatus"}, allEntries = true)
    public AuctionResponse createAuction(CreateAuctionRequest request) {
        log.info("경매 생성 요청: {}", request);

        auctionValidator.validateCreate(request);

        Auction auction = request.toEntity();
        Auction savedAuction = auctionRepository.save(auction);

        log.info("경매 저장 완료, Outbox 저장 시작: auctionId={}", savedAuction.getId());
        try {
            outboxService.saveAuctionEvent(
                    "AUCTION_CREATED",
                    savedAuction.getId(),
                    Map.of(
                            "auctionId", savedAuction.getId(),
                            "productSizeId", savedAuction.getProductSizeId(),
                            "sellerId", savedAuction.getSellerId(),
                            "status", savedAuction.getStatus().name()
                    ),
                    UUID.randomUUID()
            );
            log.info("Outbox 저장 호출 완료: auctionId={}", savedAuction.getId());
        } catch (Exception e) {
            log.error("Outbox 저장 중 예외 발생: auctionId={}", savedAuction.getId(), e);
            throw e;
        }

        log.info("경매 생성 완료: {}", savedAuction.getId());

        return AuctionResponse.from(savedAuction);
    }

    @Cacheable(value = "auctions", key = "#auctionId")
    public AuctionResponse getAuction(UUID auctionId) {
        log.info("경매 조회: {}", auctionId);

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        viewCountCacheService.incrementViewCountAsync(auctionId);

        AuctionResponse response = AuctionResponse.from(auction);
        Integer cachedViewCount = viewCountCacheService.getViewCount(auctionId, response.viewCount());

        return response.withViewCount(cachedViewCount);
    }

    public List<AuctionResponse> getAuctionsBySeller(UUID sellerId) {
        log.info("판매자 경매 목록 조회: {}", sellerId);

        List<Auction> auctions = auctionRepository.findBySellerId(sellerId);
        return auctions.stream()
                .map(AuctionResponse::fromSummary)
                .toList();
    }

    @Cacheable(value = "auctionsByStatus", key = "#status.name()")
    public List<AuctionResponse> getAuctionsByStatus(AuctionStatus status) {
        log.info("상태별 경매 목록 조회: {}", status);

        List<Auction> auctions = auctionRepository.findByStatus(status);
        return auctions.stream()
                .map(AuctionResponse::fromSummary)
                .toList();
    }

    @Transactional
    @CachePut(value = "auctions", key = "#auctionId")
    public AuctionResponse updateAuction(UUID auctionId, UpdateAuctionRequest request) {
        log.info("경매 수정 요청: {} - {}", auctionId, request);

        auctionValidator.validateUpdate(auctionId, request);

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        updateAuctionFields(auction, request);

        Auction updatedAuction = auctionRepository.save(auction);

        log.info("경매 수정 완료: {}", auctionId);

        return AuctionResponse.from(updatedAuction);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "auctions", key = "#auctionId"),
        @CacheEvict(value = "auctionsByStatus", allEntries = true)
    })
    public void cancelAuction(UUID auctionId) {
        log.info("경매 취소 요청: {}", auctionId);

        auctionValidator.validateCancel(auctionId);

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        auction.cancel();
        auctionRepository.save(auction);

        log.info("경매 취소 완료: {}", auctionId);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "auctions", key = "#auctionId"),
        @CacheEvict(value = "auctionsByStatus", allEntries = true)
    })
    public void confirmAuctionCreation(UUID auctionId) {
        log.info("경매 생성 확정: {}", auctionId);

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        auction.confirmCreation();
        auctionRepository.save(auction);

        log.info("경매 생성 확정 완료: {}", auctionId);
    }

    private void updateAuctionFields(Auction auction, UpdateAuctionRequest request) {
        auction.update(
                request.auctionTitle(),
                request.description(),
                request.startTime(),
                request.endTime(),
                request.buyNowPrice()
        );
    }
}
