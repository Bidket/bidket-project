package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.application.compensation.CompensationAction;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReopenAuctionCompensationAction implements CompensationAction {

    private final AuctionRepository auctionRepository;

    @Override
    @Transactional
    public void execute(UUID sagaId, UUID auctionId, String payload) throws Exception {
        log.info("[ReopenAuctionCompensation] 경매 재오픈 시작: sagaId={}, auctionId={}",
                sagaId, auctionId);

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() == AuctionStatus.REOPENED) {
            log.info("[ReopenAuctionCompensation] 이미 재오픈된 경매: auctionId={}, status={}",
                    auctionId, auction.getStatus());
            return;
        }

        auction.reopen();
        auctionRepository.save(auction);

        log.info("[ReopenAuctionCompensation] 경매 재오픈 완료: auctionId={}, newEndTime={}",
                auctionId, auction.getPeriod().getEndTime());

    }
}
