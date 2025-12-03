package com.bidket.auction.infrastructure.persistence.impl;

import com.bidket.auction.domain.model.Auction;
import com.bidket.auction.domain.model.AuctionStatus;
import com.bidket.auction.domain.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuctionRepositoryImpl implements AuctionRepository {

    private final AuctionJpaRepository jpaRepository;

    @Override
    public Auction save(Auction auction) {
        return jpaRepository.save(auction);
    }

    @Override
    public Optional<Auction> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Auction> findBySellerId(UUID sellerId) {
        return jpaRepository.findBySellerId(sellerId);
    }

    @Override
    public List<Auction> findByStatus(AuctionStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<Auction> findActiveAuctionsEndingBefore(LocalDateTime dateTime) {
        return jpaRepository.findActiveAuctionsEndingBefore(dateTime);
    }

    @Override
    public List<Auction> findPendingAuctionsStartingBefore(LocalDateTime dateTime) {
        return jpaRepository.findPendingAuctionsStartingBefore(dateTime);
    }

    @Override
    public void delete(Auction auction) {
        jpaRepository.delete(auction);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}
