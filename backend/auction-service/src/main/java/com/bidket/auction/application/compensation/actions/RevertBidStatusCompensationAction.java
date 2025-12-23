package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.application.compensation.CompensationAction;
import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.model.BidStatus;
import com.bidket.auction.domain.bid.repository.BidRepository;
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
public class RevertBidStatusCompensationAction implements CompensationAction {

    private final BidRepository bidRepository;

    @Override
    @Transactional
    public void execute(UUID sagaId, UUID bidId, String payload) throws Exception {
        log.info("[RevertBidStatusCompensation] 입찰 상태 복원 시작: sagaId={}, bidId={}",
                sagaId, bidId);

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.BID_NOT_FOUND));

        if (bid.getStatus() != BidStatus.WON) {
            log.info("[RevertBidStatusCompensation] 이미 WON 상태가 아닌 입찰: bidId={}, status={}",
                    bidId, bid.getStatus());
            return;
        }

        bid.markAsHighest();
        bidRepository.save(bid);

        log.info("[RevertBidStatusCompensation] 입찰 상태 복원 완료: bidId={}, status=ACTIVE",
                bidId);
    }
}
