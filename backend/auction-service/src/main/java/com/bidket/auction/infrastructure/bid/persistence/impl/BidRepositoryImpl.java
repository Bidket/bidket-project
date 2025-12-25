package com.bidket.auction.infrastructure.bid.persistence.impl;

import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.model.BidStatus;
import com.bidket.auction.domain.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BidRepositoryImpl implements BidRepository {

    private final BidJpaRepository bidJpaRepository;

    @Override
    @CacheEvict(value = {"highestBid", "bids"}, key = "#bid.auctionId")
    public Bid save(Bid bid) {
        return bidJpaRepository.save(bid);
    }

    @Override
    public Optional<Bid> findById(UUID id) {
        return bidJpaRepository.findById(id);
    }

    @Override
    @Cacheable(value = "bids", key = "#auctionId")
    public List<Bid> findByAuctionId(UUID auctionId) {
        return bidJpaRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId);
    }
    

    @Override
    public List<Bid> findByBidderId(UUID bidderId) {
        return bidJpaRepository.findByBidderIdOrderByCreatedAtDesc(bidderId);
    }

    @Override
    @Cacheable(value = "highestBid", key = "#auctionId", unless = "#result.isEmpty()")
    public Optional<Bid> findHighestBidByAuctionId(UUID auctionId) {
        // 최적화된 네이티브 쿼리 사용
        return bidJpaRepository.findHighestBidOptimized(auctionId);
    }

    @Override
    public List<Bid> findByAuctionIdAndBidderId(UUID auctionId, UUID bidderId) {
        return bidJpaRepository.findByAuctionIdAndBidderId(auctionId, bidderId);
    }

    @Override
    public List<Bid> findByStatus(BidStatus status) {
        return bidJpaRepository.findByStatus(status);
    }

    @Override
    public boolean existsByAuctionIdAndBidderId(UUID auctionId, UUID bidderId) {
        return bidJpaRepository.existsByAuctionIdAndBidderId(auctionId, bidderId);
    }

    @Override
    public Optional<Bid> findByIdempotencyKey(String idempotencyKey) {
        return bidJpaRepository.findByBidMetadata_IdempotencyKey(idempotencyKey);
    }

    @Override
    public void delete(Bid bid) {
        bidJpaRepository.delete(bid);
    }

    @Override
    public long countByAuctionId(UUID auctionId) {
        return bidJpaRepository.countByAuctionId(auctionId);
    }
}
