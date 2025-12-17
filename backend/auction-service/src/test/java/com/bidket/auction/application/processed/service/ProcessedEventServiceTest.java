package com.bidket.auction.application.processed.service;

import com.bidket.auction.domain.processed.model.ProcessedEvent;
import com.bidket.auction.domain.processed.repository.ProcessedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessedEventService 단위 테스트")
class ProcessedEventServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private ProcessedEventService processedEventService;

    @Test
    @DisplayName("이미 처리된 이벤트 여부를 확인한다")
    void shouldCheckProcessed() {
        UUID eventId = UUID.randomUUID();
        given(processedEventRepository.existsById(eventId)).willReturn(true);

        boolean result = processedEventService.isProcessed(eventId);

        assertThat(result).isTrue();
        verify(processedEventRepository).existsById(eventId);
    }

    @Test
    @DisplayName("이벤트를 처리 완료 상태로 기록한다")
    void shouldMarkProcessed() {
        UUID eventId = UUID.randomUUID();
        given(processedEventRepository.save(any(ProcessedEvent.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        processedEventService.markProcessed(eventId, "AUCTION_ENDED", null);

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }
}
