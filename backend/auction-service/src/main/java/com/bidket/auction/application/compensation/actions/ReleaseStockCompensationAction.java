package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.application.compensation.CompensationAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 재고 복원 보상 액션
 * BACKLOG.md SAGA-002 참조
 *
 * 역할:
 * - Product Service에 재고 복원 요청
 * - 결제 타임아웃으로 경매가 재오픈될 때 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseStockCompensationAction implements CompensationAction {

    // TODO: Product Service 연동을 위한 Producer 추가 (확장 버전)
    // private final ProductEventProducer productEventProducer;

    @Override
    @Transactional
    public void execute(UUID sagaId, UUID productSizeId, String payload) throws Exception {
        log.info("[ReleaseStockCompensation] 재고 복원 시작: sagaId={}, productSizeId={}",
                sagaId, productSizeId);

        // MVP: 로깅만 수행
        // 확장: Product Service에 RELEASE_STOCK_REQUESTED 이벤트 발행
        // productEventProducer.publishReleaseStockRequest(productSizeId, sagaId);

        log.info("[ReleaseStockCompensation] 재고 복원 완료 (MVP - 로깅만): productSizeId={}", productSizeId);
    }
}
