package com.bidket.auction.application.bid.service;

import com.bidket.auction.application.bid.dto.request.CreateBidRequest;
import com.bidket.auction.application.bid.dto.response.BidListResponse;
import com.bidket.auction.application.bid.dto.response.BidResponse;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.repository.BidRepository;
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
    public Bid placeBid(UUID auctionId, UUID bidderId, Long amount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다: " + auctionId));

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 상태의 경매에만 입찰할 수 있습니다");
        }

        if (auction.getSellerId().equals(bidderId)) {
            throw new IllegalArgumentException("본인의 경매에는 입찰할 수 없습니다");
        }

        Long minimumBid = auction.getPriceInfo().getCurrentPrice() + auction.getPriceInfo().getBidIncrement();
        if (amount < minimumBid) {
            throw new IllegalArgumentException(
                String.format("최소 입찰가는 %d원입니다 (현재가: %d원 + 입찰 단위: %d원)",
                    minimumBid,
                    auction.getPriceInfo().getCurrentPrice(),
                    auction.getPriceInfo().getBidIncrement())
            );
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
                .orElseThrow(() -> new IllegalArgumentException("입찰을 찾을 수 없습니다: " + bidId));
        return BidResponse.from(bid);
    }

    @Transactional
    public void cancelBid(UUID bidId, UUID bidderId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new IllegalArgumentException("입찰을 찾을 수 없습니다: " + bidId));

        if (!bid.getBidderId().equals(bidderId)) {
            throw new IllegalArgumentException("본인의 입찰만 취소할 수 있습니다");
        }

        bid.cancel();
        bidRepository.save(bid);

        log.info("입찰 취소 완료 - 입찰 ID: {}, 입찰자: {}", bidId, bidderId);
    }
}


