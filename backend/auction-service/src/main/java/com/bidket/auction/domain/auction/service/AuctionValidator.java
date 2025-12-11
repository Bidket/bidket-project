package com.bidket.auction.domain.auction.service;

import com.bidket.auction.application.auction.dto.request.CreateAuctionRequest;
import com.bidket.auction.application.auction.dto.request.UpdateAuctionRequest;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuctionValidator {

    private final AuctionRepository auctionRepository;

    public void validateCreate(CreateAuctionRequest request) {
        validateTimeRange(request.startTime(), request.endTime());
        validateBuyNowPrice(request.startPrice(), request.buyNowPrice());
    }

    public void validateUpdate(UUID auctionId, UpdateAuctionRequest request) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.PENDING) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_AUCTION_STATUS);
        }

        if (LocalDateTime.now().isAfter(auction.getPeriod().getStartTime())) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_AUCTION_STATUS);
        }

        LocalDateTime startTime = request.startTime() != null ? request.startTime() : auction.getPeriod().getStartTime();
        LocalDateTime endTime = request.endTime() != null ? request.endTime() : auction.getPeriod().getEndTime();
        validateTimeRange(startTime, endTime);

        if (request.buyNowPrice() != null) {
            validateBuyNowPrice(auction.getPriceInfo().getStartPrice(), request.buyNowPrice());
        }
    }

    public void validateCancel(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() == AuctionStatus.PENDING) {
            return;
        }

        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            if (auction.getStats().getTotalBidsCount() > 0) {
                throw new AuctionDomainException(AuctionErrorCode.CANNOT_CANCEL_WITH_BIDS);
            }
            return;
        }

        throw new AuctionDomainException(AuctionErrorCode.INVALID_AUCTION_STATUS);
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minStartTime = now.plusHours(1);
        LocalDateTime minEndTime = startTime.plusHours(1);
        LocalDateTime maxEndTime = startTime.plusDays(7);

        if (startTime.isBefore(minStartTime)) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_TIME_RANGE);
        }

        if (endTime.isBefore(minEndTime)) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_TIME_RANGE);
        }

        if (endTime.isAfter(maxEndTime)) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_AUCTION_PERIOD);
        }
    }

    private void validateBuyNowPrice(Long startPrice, Long buyNowPrice) {
        if (buyNowPrice != null && buyNowPrice <= startPrice) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_BUY_NOW_PRICE);
        }
    }
}


