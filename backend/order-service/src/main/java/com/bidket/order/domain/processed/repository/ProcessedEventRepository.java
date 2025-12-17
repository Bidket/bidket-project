package com.bidket.order.domain.processed.repository;

import com.bidket.order.domain.processed.model.ProcessedEvent;
import java.util.UUID;

/**
 * 처리된 이벤트 저장소
 */
public interface ProcessedEventRepository {

    /**
     * 이벤트 ID가 이미 처리되었는지 확인
     */
    boolean existsById(UUID eventId);

    /**
     * 처리된 이벤트 저장
     */
    ProcessedEvent save(ProcessedEvent processedEvent);
}
