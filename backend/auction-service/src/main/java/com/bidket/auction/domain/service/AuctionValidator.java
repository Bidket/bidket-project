package com.bidket.auction.domain.service;

import com.bidket.auction.application.dto.request.CreateAuctionRequest;
import com.bidket.auction.application.dto.request.UpdateAuctionRequest;
import com.bidket.auction.domain.model.Auction;
import com.bidket.auction.domain.model.AuctionStatus;
import com.bidket.auction.domain.repository.AuctionRepository;
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
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다: " + auctionId));

        if (auction.getStatus() != AuctionStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 수정할 수 있습니다");
        }

        if (LocalDateTime.now().isAfter(auction.getStartTime())) {
            throw new IllegalStateException("시작 시간이 이미 지난 경매는 수정할 수 없습니다");
        }

        LocalDateTime startTime = request.startTime() != null ? request.startTime() : auction.getStartTime();
        LocalDateTime endTime = request.endTime() != null ? request.endTime() : auction.getEndTime();
        validateTimeRange(startTime, endTime);

        if (request.buyNowPrice() != null) {
            validateBuyNowPrice(auction.getStartPrice(), request.buyNowPrice());
        }
    }

    public void validateCancel(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다: " + auctionId));

        if (auction.getStatus() == AuctionStatus.PENDING) {
            return;
        }

        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            if (auction.getTotalBidsCount() > 0) {
                throw new IllegalStateException("입찰이 있는 경매는 취소할 수 없습니다");
            }
            return;
        }

        throw new IllegalStateException("취소할 수 없는 상태입니다: " + auction.getStatus());
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minStartTime = now.plusHours(1);
        LocalDateTime minEndTime = startTime.plusHours(1);
        LocalDateTime maxEndTime = startTime.plusDays(7);

        if (startTime.isBefore(minStartTime)) {
            throw new IllegalArgumentException("시작 시간은 현재 시간 + 1시간 이후여야 합니다");
        }

        if (endTime.isBefore(minEndTime)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간 + 1시간 이후여야 합니다");
        }

        if (endTime.isAfter(maxEndTime)) {
            throw new IllegalArgumentException("경매 기간은 최대 7일까지 가능합니다");
        }
    }

    private void validateBuyNowPrice(Long startPrice, Long buyNowPrice) {
        if (buyNowPrice != null && buyNowPrice <= startPrice) {
            throw new IllegalArgumentException("즉시구매가는 시작가보다 커야 합니다");
        }
    }
}
