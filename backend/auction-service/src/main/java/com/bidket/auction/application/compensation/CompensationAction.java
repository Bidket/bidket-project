package com.bidket.auction.application.compensation;

import java.util.UUID;

/**
 * 보상 액션 인터페이스
 * 각 보상 타입별로 구현체를 만들어야 함
 */
@FunctionalInterface
public interface CompensationAction {

    /**
     * 보상 실행
     *
     * @param sagaId      Saga ID
     * @param aggregateId 대상 엔티티 ID
     * @param payload     보상 실행 데이터 (JSON)
     * @throws Exception 보상 실행 실패 시
     */
    void execute(UUID sagaId, UUID aggregateId, String payload) throws Exception;
}
