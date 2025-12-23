package com.bidket.auction.infrastructure.config;

import com.bidket.auction.application.compensation.CompensationExecutor;
import com.bidket.auction.application.compensation.actions.CancelOrderCompensationAction;
import com.bidket.auction.application.compensation.actions.CancelPaymentCompensationAction;
import com.bidket.auction.application.compensation.actions.ReopenAuctionCompensationAction;
import com.bidket.auction.application.compensation.actions.RevertBidStatusCompensationAction;
import com.bidket.auction.domain.compensation.model.CompensationType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Slf4j
@Configuration
@EnableRetry
@RequiredArgsConstructor
public class CompensationConfig {

    private final CompensationExecutor compensationExecutor;
    private final ReopenAuctionCompensationAction reopenAuctionAction;
    private final CancelOrderCompensationAction cancelOrderAction;
    private final RevertBidStatusCompensationAction revertBidStatusAction;
    private final CancelPaymentCompensationAction cancelPaymentAction;

    @PostConstruct
    public void registerCompensationActions() {
        log.info("[CompensationConfig] 보상 액션 등록 시작");

        compensationExecutor.registerCompensationAction(
                CompensationType.REOPEN_AUCTION,
                reopenAuctionAction
        );

        compensationExecutor.registerCompensationAction(
                CompensationType.CANCEL_ORDER,
                cancelOrderAction
        );

        compensationExecutor.registerCompensationAction(
                CompensationType.REVERT_BID_STATUS,
                revertBidStatusAction
        );

        compensationExecutor.registerCompensationAction(
                CompensationType.CANCEL_PAYMENT,
                cancelPaymentAction
        );

        log.info("[CompensationConfig] 보상 액션 등록 완료: count={}",
                4);
    }
}
