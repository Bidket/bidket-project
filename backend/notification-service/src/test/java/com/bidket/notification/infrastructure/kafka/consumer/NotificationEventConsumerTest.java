package com.bidket.notification.infrastructure.kafka.consumer;

import com.bidket.notification.application.service.EventNotificationService;
import com.bidket.notification.domain.model.EventTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * NotificationEventConsumer 단위 테스트
 * Kafka 없이 JUnit5 + Mockito 사용
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventConsumer 단위 테스트")
class NotificationEventConsumerTest {

    @Mock
    private EventNotificationService eventNotificationService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private NotificationEventConsumer notificationEventConsumer;

    @Test
    @DisplayName("정상 처리: service 호출 및 ack 확인")
    void shouldProcessEventSuccessfullyAndAcknowledge() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String topic = "notification.queue";
        int partition = 0;
        long offset = 100L;
        String userIdString = userId.toString();

        EventTemplate eventTemplate = createEventTemplate(eventId, userId, "near_turn");

        // when
        notificationEventConsumer.consumeQueueEvent(
                eventTemplate, userIdString, topic, partition, offset, acknowledgment
        );

        // then
        verify(eventNotificationService).processEvent(eventTemplate, topic);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("정상 처리: acknowledgment가 null인 경우 ack 호출 안함")
    void shouldProcessEventSuccessfullyWithoutAcknowledgment() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String topic = "notification.auction";
        int partition = 0;
        long offset = 100L;
        String userIdString = userId.toString();

        EventTemplate eventTemplate = createEventTemplate(eventId, userId, "outbid");

        // when
        notificationEventConsumer.consumeAuctionEvent(
                eventTemplate, userIdString, topic, partition, offset, null
        );

        // then
        verify(eventNotificationService).processEvent(eventTemplate, topic);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("eventTemplate null: IllegalArgumentException 발생, ack 호출 안함")
    void shouldThrowExceptionWhenEventTemplateIsNull() {
        // given
        String topic = "notification.queue";
        int partition = 0;
        long offset = 100L;
        String userIdString = UUID.randomUUID().toString();

        // when & then
        assertThatThrownBy(() -> notificationEventConsumer.consumeQueueEvent(
                null, userIdString, topic, partition, offset, acknowledgment
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payload는 필수입니다");

        verify(eventNotificationService, never()).processEvent(any(), any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("eventType null: IllegalArgumentException 발생, ack 호출 안함")
    void shouldThrowExceptionWhenEventTypeIsNull() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String topic = "notification.order";
        int partition = 0;
        long offset = 100L;
        String userIdString = userId.toString();

        EventTemplate eventTemplate = EventTemplate.builder()
                .eventId(eventId)
                .eventType(null) // null
                .userId(userId)
                .occurredAt(LocalDateTime.now())
                .source("order-service")
                .data(new HashMap<>())
                .build();

        // when & then
        assertThatThrownBy(() -> notificationEventConsumer.consumeOrderEvent(
                eventTemplate, userIdString, topic, partition, offset, acknowledgment
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventType은 필수입니다");

        verify(eventNotificationService, never()).processEvent(any(), any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("eventType blank: IllegalArgumentException 발생, ack 호출 안함")
    void shouldThrowExceptionWhenEventTypeIsBlank() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String topic = "notification.queue";
        int partition = 0;
        long offset = 100L;
        String userIdString = userId.toString();

        EventTemplate eventTemplate = EventTemplate.builder()
                .eventId(eventId)
                .eventType("   ") // blank
                .userId(userId)
                .occurredAt(LocalDateTime.now())
                .source("queue-service")
                .data(new HashMap<>())
                .build();

        // when & then
        assertThatThrownBy(() -> notificationEventConsumer.consumeQueueEvent(
                eventTemplate, userIdString, topic, partition, offset, acknowledgment
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventType은 필수입니다");

        verify(eventNotificationService, never()).processEvent(any(), any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("service 처리 실패: 예외 전파, ack 호출 안함")
    void shouldPropagateExceptionWhenServiceFails() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String topic = "notification.auction";
        int partition = 0;
        long offset = 100L;
        String userIdString = userId.toString();

        EventTemplate eventTemplate = createEventTemplate(eventId, userId, "closed");

        RuntimeException serviceException = new RuntimeException("Service processing failed");
        doThrow(serviceException).when(eventNotificationService)
                .processEvent(any(EventTemplate.class), eq(topic));

        // when & then
        assertThatThrownBy(() -> notificationEventConsumer.consumeAuctionEvent(
                eventTemplate, userIdString, topic, partition, offset, acknowledgment
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service processing failed");

        verify(eventNotificationService).processEvent(eventTemplate, topic);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("모든 토픽 리스너가 동일한 consumeEvent 메서드 호출 확인")
    void shouldCallConsumeEventForAllTopics() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();
        int partition = 0;
        long offset = 100L;

        EventTemplate eventTemplate = createEventTemplate(eventId, userId, "near_turn");

        // when
        notificationEventConsumer.consumeQueueEvent(
                eventTemplate, userIdString, "notification.queue", partition, offset, acknowledgment
        );
        notificationEventConsumer.consumeAuctionEvent(
                eventTemplate, userIdString, "notification.auction", partition, offset, acknowledgment
        );
        notificationEventConsumer.consumeOrderEvent(
                eventTemplate, userIdString, "notification.order", partition, offset, acknowledgment
        );

        // then
        verify(eventNotificationService, times(3)).processEvent(any(EventTemplate.class), anyString());
        verify(acknowledgment, times(3)).acknowledge();
    }

    @Test
    @DisplayName("admitted 이벤트 정상 처리")
    void shouldProcessAdmittedEventSuccessfully() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String topic = "notification.queue";
        String userIdString = userId.toString();

        EventTemplate eventTemplate = createEventTemplate(eventId, userId, "admitted");

        // when
        notificationEventConsumer.consumeQueueEvent(
                eventTemplate, userIdString, topic, 0, 100L, acknowledgment
        );

        // then
        verify(eventNotificationService).processEvent(eventTemplate, topic);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("outbid 이벤트 정상 처리")
    void shouldProcessOutbidEventSuccessfully() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String topic = "notification.auction";
        String userIdString = userId.toString();

        EventTemplate eventTemplate = createEventTemplate(eventId, userId, "outbid");

        // when
        notificationEventConsumer.consumeAuctionEvent(
                eventTemplate, userIdString, topic, 0, 100L, acknowledgment
        );

        // then
        verify(eventNotificationService).processEvent(eventTemplate, topic);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("closed 이벤트 정상 처리")
    void shouldProcessClosedEventSuccessfully() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String topic = "notification.auction";
        String userIdString = userId.toString();

        EventTemplate eventTemplate = createEventTemplate(eventId, userId, "closed");

        // when
        notificationEventConsumer.consumeAuctionEvent(
                eventTemplate, userIdString, topic, 0, 100L, acknowledgment
        );

        // then
        verify(eventNotificationService).processEvent(eventTemplate, topic);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("payment_required 이벤트 정상 처리")
    void shouldProcessPaymentRequiredEventSuccessfully() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String topic = "notification.order";
        String userIdString = userId.toString();

        EventTemplate eventTemplate = createEventTemplate(eventId, userId, "payment_required");

        // when
        notificationEventConsumer.consumeOrderEvent(
                eventTemplate, userIdString, topic, 0, 100L, acknowledgment
        );

        // then
        verify(eventNotificationService).processEvent(eventTemplate, topic);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("paid 이벤트 정상 처리")
    void shouldProcessPaidEventSuccessfully() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String topic = "notification.order";
        String userIdString = userId.toString();

        EventTemplate eventTemplate = createEventTemplate(eventId, userId, "paid");

        // when
        notificationEventConsumer.consumeOrderEvent(
                eventTemplate, userIdString, topic, 0, 100L, acknowledgment
        );

        // then
        verify(eventNotificationService).processEvent(eventTemplate, topic);
        verify(acknowledgment).acknowledge();
    }

    /**
     * 테스트용 EventTemplate 생성 헬퍼 메서드
     */
    private EventTemplate createEventTemplate(UUID eventId, UUID userId, String eventType) {
        Map<String, Object> data = new HashMap<>();
        return EventTemplate.builder()
                .eventId(eventId)
                .eventType(eventType)
                .userId(userId)
                .occurredAt(LocalDateTime.now())
                .source("test-service")
                .data(data)
                .build();
    }
}

