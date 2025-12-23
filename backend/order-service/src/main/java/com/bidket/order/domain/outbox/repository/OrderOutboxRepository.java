package com.bidket.order.domain.outbox.repository;

import com.bidket.order.domain.outbox.model.OrderOutbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OrderOutbox 도메인 Repository 인터페이스
 */
public interface OrderOutboxRepository {

    /**
     * OutBox 저장
     *
     * @param outbox OrderOutbox 도메인 객체
     * @return 저장된 OrderOutbox
     */
    OrderOutbox save(OrderOutbox outbox);

    /**
     * 발행 가능한 OutBox 조회
     * - PENDING 또는 FAILED 상태
     * - 백오프 시간 경과
     * - 최대 재시도 횟수 미초과
     *
     * @param batchSize 한 번에 조회할 최대 개수
     * @return 발행 가능한 OutBox 리스트
     */
    List<OrderOutbox> findReadyToPublish(int batchSize);

    /**
     * ID로 OutBox 조회
     *
     * @param id OutBox ID
     * @return OrderOutbox Optional
     */
    Optional<OrderOutbox> findById(UUID id);
}
