package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.application.compensation.CompensationAction;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 경매 재오픈 보상 액션
 * BACKLOG.md line 758 참조
 */
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

        // 1. 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        // 2. Idempotency 체크: 이미 재오픈된 경매는 건너뜀
        if (auction.getStatus() == com.bidket.auction.domain.auction.model.AuctionStatus.REOPENED) {
            log.info("[ReopenAuctionCompensation] 이미 재오픈된 경매: auctionId={}, status={}",
                    auctionId, auction.getStatus());
            return;
        }

        // 3. 경매 재오픈 (winner 정보 초기화 + endTime 연장)
        auction.reopen();
        auctionRepository.save(auction);

        log.info("[ReopenAuctionCompensation] 경매 재오픈 완료: auctionId={}, newEndTime={}",
                auctionId, auction.getPeriod().getEndTime());

        // 4. TODO: AUCTION_REOPENED 이벤트 발행 (확장 버전)
        // eventPublisher.publish(AuctionReopenedEvent.of(auction));
    }
}
