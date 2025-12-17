package com.bidket.auction.application.processed.service;

import com.bidket.auction.domain.processed.model.ProcessedEvent;
import com.bidket.auction.domain.processed.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional(readOnly = true)
    public boolean isProcessed(UUID eventId) {
        return processedEventRepository.existsById(eventId);
    }

    @Transactional
    public void markProcessed(UUID eventId, String eventType, UUID correlationId) {
        ProcessedEvent processedEvent = ProcessedEvent.of(eventId, eventType, correlationId, null);
        processedEventRepository.save(processedEvent);
        log.debug("이벤트 처리 완료 기록: eventId={}, eventType={}", eventId, eventType);
    }
}
