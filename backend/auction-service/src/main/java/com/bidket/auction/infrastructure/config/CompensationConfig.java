package com.bidket.auction.infrastructure.config;

import com.bidket.auction.application.compensation.CompensationExecutor;
import com.bidket.auction.application.compensation.actions.CancelOrderCompensationAction;
import com.bidket.auction.application.compensation.actions.ReleaseStockCompensationAction;
import com.bidket.auction.application.compensation.actions.ReopenAuctionCompensationAction;
import com.bidket.auction.application.compensation.actions.RevertBidStatusCompensationAction;
import com.bidket.auction.domain.compensation.model.CompensationType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 보상 트랜잭션 설정
 * 각 CompensationType에 대한 액션을 등록
 */
@Slf4j
@Configuration
@EnableRetry
@RequiredArgsConstructor
public class CompensationConfig {

    private final CompensationExecutor compensationExecutor;
    private final ReopenAuctionCompensationAction reopenAuctionAction;
    private final CancelOrderCompensationAction cancelOrderAction;
    private final RevertBidStatusCompensationAction revertBidStatusAction;
    private final ReleaseStockCompensationAction releaseStockAction;

    @PostConstruct
    public void registerCompensationActions() {
        log.info("[CompensationConfig] 보상 액션 등록 시작");

        // 1. 경매 재오픈
        compensationExecutor.registerCompensationAction(
                CompensationType.REOPEN_AUCTION,
                reopenAuctionAction
        );

        // 2. 주문 취소
        compensationExecutor.registerCompensationAction(
                CompensationType.CANCEL_ORDER,
                cancelOrderAction
        );

        // 3. 입찰 상태 복원
        compensationExecutor.registerCompensationAction(
                CompensationType.REVERT_BID_STATUS,
                revertBidStatusAction
        );

        // 4. 재고 복원
        compensationExecutor.registerCompensationAction(
                CompensationType.RESTORE_STOCK,
                releaseStockAction
        );

        // TODO: 추가 보상 액션 등록
        // - CANCEL_PAYMENT: 결제 취소

        log.info("[CompensationConfig] 보상 액션 등록 완료: count={}",
                4);
    }
}
