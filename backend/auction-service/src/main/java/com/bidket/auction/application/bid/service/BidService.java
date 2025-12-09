package com.bidket.auction.application.bid.service;

import com.bidket.auction.application.bid.dto.request.CreateBidRequest;
import com.bidket.auction.application.bid.dto.response.BidListResponse;
import com.bidket.auction.application.bid.dto.response.BidResponse;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.repository.BidRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import com.bidket.auction.global.exception.BidDomainException;
import com.bidket.auction.global.exception.BidErrorCode;
import com.bidket.auction.infrastructure.retry.RetryOnOptimisticLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;

    @Transactional
    @RetryOnOptimisticLock
    public Bid placeBid(UUID auctionId, UUID bidderId, Long amount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_ACTIVE);
        }

        if (auction.getSellerId().equals(bidderId)) {
            throw new BidDomainException(BidErrorCode.SELF_BID_NOT_ALLOWED);
        }

        Long minimumBid = auction.getPriceInfo().getCurrentPrice() + auction.getPriceInfo().getBidIncrement();
        if (amount < minimumBid) {
            throw new BidDomainException(BidErrorCode.BID_AMOUNT_TOO_LOW);
        }

        Optional<Bid> previousHighestBid = bidRepository.findHighestBidByAuctionId(auctionId);
        if (previousHighestBid.isPresent()) {
            Bid prevBid = previousHighestBid.get();
            prevBid.markAsOutbid();
            bidRepository.save(prevBid);
        }

        Bid newBid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(amount)
                .build();
        newBid.markAsHighest();
        
        Bid savedBid = bidRepository.save(newBid);

        auction.updateCurrentPrice(amount);
        auctionRepository.save(auction);

        log.info("입찰 등록 완료 - 경매 ID: {}, 입찰자: {}, 금액: {}", auctionId, bidderId, amount);
        
        return savedBid;
    }

    @Transactional
    public BidResponse createBid(UUID bidderId, CreateBidRequest request) {
        Bid bid = placeBid(request.auctionId(), bidderId, request.amount());
        return BidResponse.from(bid);
    }

    public BidListResponse getBidsByAuction(UUID auctionId) {
        List<Bid> bids = bidRepository.findByAuctionId(auctionId);
        List<BidResponse> bidResponses = bids.stream()
                .map(BidResponse::from)
                .toList();
        
        return BidListResponse.of(bidResponses, bids.size());
    }

    public BidListResponse getMyBids(UUID bidderId) {
        List<Bid> bids = bidRepository.findByBidderId(bidderId);
        List<BidResponse> bidResponses = bids.stream()
                .map(BidResponse::from)
                .toList();
        
        return BidListResponse.of(bidResponses, bids.size());
    }

    public BidResponse getBidById(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BidDomainException(BidErrorCode.BID_NOT_FOUND));
        return BidResponse.from(bid);
    }

    @Transactional
    public void cancelBid(UUID bidId, UUID bidderId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BidDomainException(BidErrorCode.BID_NOT_FOUND));

        if (!bid.getBidderId().equals(bidderId)) {
            throw new BidDomainException(BidErrorCode.NOT_BID_OWNER);
        }

        if (bid.isHighest()) {
            throw new BidDomainException(BidErrorCode.CANNOT_CANCEL_HIGHEST_BID);
        }
        bid.cancel();
        bidRepository.save(bid);

        log.info("입찰 취소 완료 - 입찰 ID: {}, 입찰자: {}", bidId, bidderId);
    }

    @Transactional
    @RetryOnOptimisticLock
    public Bid buyNow(UUID auctionId, UUID bidderId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_ACTIVE);
        }

        if (auction.getSellerId().equals(bidderId)) {
            throw new BidDomainException(BidErrorCode.SELF_BID_NOT_ALLOWED);
        }

        Long buyNowPrice = auction.getPriceInfo().getBuyNowPrice();
        if (buyNowPrice == null) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_BUY_NOW_PRICE);
        }

        Optional<Bid> previousHighestBid = bidRepository.findHighestBidByAuctionId(auctionId);
        if (previousHighestBid.isPresent()) {
            Bid prevBid = previousHighestBid.get();
            prevBid.markAsOutbid();
            bidRepository.save(prevBid);
        }

        Bid buyNowBid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(buyNowPrice)
                .build();
        buyNowBid.markAsHighest();

        Bid savedBid = bidRepository.save(buyNowBid);

        auction.updateCurrentPrice(buyNowPrice);
        auction.end(true);
        auctionRepository.save(auction);

        log.info("즉시 구매 완료 - 경매 ID: {}, 구매자: {}, 금액: {}", auctionId, bidderId, buyNowPrice);

        return savedBid;
    }
}


