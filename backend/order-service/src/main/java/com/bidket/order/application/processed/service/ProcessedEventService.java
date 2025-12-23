package com.bidket.order.application.processed.service;

import com.bidket.order.domain.processed.model.ProcessedEvent;
import com.bidket.order.domain.processed.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 처리된 이벤트 서비스
 * 멱등성 보장을 위한 이벤트 처리 추적
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    /**
     * 이벤트가 이미 처리되었는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isProcessed(UUID eventId) {
        return processedEventRepository.existsById(eventId);
    }

    /**
     * 이벤트를 처리 완료로 표시
     */
    @Transactional
    public void markProcessed(UUID eventId, String eventType, UUID correlationId) {
        ProcessedEvent processedEvent = ProcessedEvent.of(eventId, eventType, correlationId, null);
        processedEventRepository.save(processedEvent);
        log.debug("이벤트 처리 완료 기록: eventId={}, eventType={}", eventId, eventType);
    }
}
