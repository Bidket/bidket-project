package com.bidket.notification.application.service;

import com.bidket.notification.domain.model.EventTemplate;
import com.bidket.notification.domain.model.NotificationChannel;
import com.bidket.notification.infrastructure.external.AuctionServiceClient;
import com.bidket.notification.infrastructure.external.EmailSender;
import com.bidket.notification.infrastructure.external.UserServiceClient;
import com.bidket.notification.infrastructure.persistence.entity.Notification;
import com.bidket.notification.infrastructure.persistence.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * EventNotificationService 단위 테스트
 * Kafka 없이 JUnit5 + Mockito 사용
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventNotificationService 단위 테스트")
class EventNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuctionServiceClient auctionServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private EmailSender emailSender;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventNotificationService eventNotificationService;

    @Test
    @DisplayName("정상 처리: Repository.save() 및 AuctionServiceClient 호출 확인")
    void shouldProcessEventSuccessfully() throws Exception {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        String topic = "notification.queue";
        LocalDateTime occurredAt = LocalDateTime.of(2025, 12, 18, 9, 0);

        // 서비스 구현: eventTemplate.data()에서 "auctionId"를 추출하여 사용
        // extractUUID()는 String 또는 UUID 타입 모두 처리 가능
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId); // UUID 타입으로 저장 (실제 Kafka 이벤트와 유사)
        data.put("position", 5);

        EventTemplate eventTemplate = EventTemplate.builder()
                .eventId(eventId)
                .eventType("near_turn")
                .userId(userId)
                .occurredAt(occurredAt)
                .source("queue-service")
                .data(data)
                .build();

        // 멱등성 체크: 존재하지 않음
        given(notificationRepository.existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP))
                .willReturn(false);

        // 경매 정보 조회 성공
        AuctionServiceClient.AuctionInfo auctionInfo = new AuctionServiceClient.AuctionInfo();
        auctionInfo.setId(auctionId);
        auctionInfo.setName("테스트 경매");
        given(auctionServiceClient.getAuctionInfo(auctionId))
                .willReturn(auctionInfo);

        // 서비스가 createPayload()에서 objectMapper.writeValueAsString()을 호출하여 payload 생성
        String expectedPayload = "{\"auctionId\":\"" + auctionId + "\",\"position\":5}";
        given(objectMapper.writeValueAsString(data))
                .willReturn(expectedPayload);

        // Repository.save()는 전달받은 객체를 그대로 반환
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        eventNotificationService.processEvent(eventTemplate, topic);

        // then
        // 1. 멱등성 체크 호출 확인
        verify(notificationRepository).existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP);

        // 2. 경매 정보 조회 호출 확인
        verify(auctionServiceClient).getAuctionInfo(auctionId);

        // 3. Repository.save() 호출 확인
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        // 4. 저장된 알림 내용 검증
        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUserId()).isEqualTo(userId);
        assertThat(savedNotification.getEventId()).isEqualTo(eventId);
        assertThat(savedNotification.getType()).isEqualTo("near_turn");
        assertThat(savedNotification.getCategory()).isEqualTo("QUEUE");
        assertThat(savedNotification.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(savedNotification.getTitle()).isEqualTo("대기 순번 임박");
        assertThat(savedNotification.getMessage()).contains("5번");
        assertThat(savedNotification.getSource()).isEqualTo("queue-service");
        assertThat(savedNotification.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(savedNotification.getPayload()).isEqualTo(expectedPayload);
    }

    @Test
    @DisplayName("eventId 중복 멱등성: Repository.save() 호출되지 않음")
    void shouldSkipProcessingWhenEventIdAlreadyExists() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        String topic = "notification.queue";
        LocalDateTime occurredAt = LocalDateTime.of(2025, 12, 18, 9, 0);

        // 서비스 구현: eventTemplate.data()에서 "auctionId"를 추출하여 사용
        // extractUUID()는 String 또는 UUID 타입 모두 처리 가능
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId); // UUID 타입으로 저장 (실제 Kafka 이벤트와 유사)
        data.put("position", 5);

        EventTemplate eventTemplate = EventTemplate.builder()
                .eventId(eventId)
                .eventType("near_turn")
                .userId(userId)
                .occurredAt(occurredAt)
                .source("queue-service")
                .data(data)
                .build();

        // 멱등성 체크: 이미 존재함
        given(notificationRepository.existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP))
                .willReturn(true);

        // when
        eventNotificationService.processEvent(eventTemplate, topic);

        // then
        // 1. 멱등성 체크 호출 확인
        verify(notificationRepository).existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP);

        // 2. Repository.save() 호출되지 않음 확인
        verify(notificationRepository, never()).save(any(Notification.class));

        // 3. AuctionServiceClient 호출되지 않음 확인
        verify(auctionServiceClient, never()).getAuctionInfo(any(UUID.class));
    }

    @Test
    @DisplayName("외부 발송 실패: AuctionServiceClient 예외 전파로 재시도 트리거, Repository.save() 호출되지 않음")
    void shouldPropagateExceptionToTriggerRetryWhenExternalServiceThrowsException() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        String topic = "notification.queue";
        LocalDateTime occurredAt = LocalDateTime.of(2025, 12, 18, 9, 0);

        // 서비스 구현: eventTemplate.data()에서 "auctionId"를 추출하여 사용
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId); // UUID 타입으로 저장 (다른 테스트와 일관성 유지)
        data.put("position", 3);

        EventTemplate eventTemplate = EventTemplate.builder()
                .eventId(eventId)
                .eventType("near_turn")
                .userId(userId)
                .occurredAt(occurredAt)
                .source("queue-service")
                .data(data)
                .build();

        // 멱등성 체크: 존재하지 않음
        given(notificationRepository.existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP))
                .willReturn(false);

        // 경매 정보 조회 실패 (예외 throw)
        // 현재 구현: AuctionServiceClient.getAuctionInfo()는 예외를 catch해서 null을 반환함
        // 하지만 향후 변경/리팩토링으로 예외가 전파될 가능성을 대비한 방어적 테스트
        // 정책: 외부 서비스 실패 시 예외를 전파하여 Kafka 재시도/DLT 트리거
        RuntimeException externalException = new RuntimeException("Auction Service unavailable");
        given(auctionServiceClient.getAuctionInfo(auctionId))
                .willThrow(externalException);
        
        // 예외가 발생하면 createPayload()는 호출되지 않아야 하지만, 
        // 혹시 호출되더라도 예외가 먼저 전파되므로 objectMapper mock은 불필요
        // (하지만 테스트 안정성을 위해 설정해도 무방)

        // when & then
        // 예외가 전파되어 Kafka 재시도를 트리거해야 함 (processEvent의 catch 블록에서 예외 전파)
        assertThatThrownBy(() -> eventNotificationService.processEvent(eventTemplate, topic))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Auction Service unavailable");

        // 1. 멱등성 체크 호출 확인
        verify(notificationRepository).existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP);

        // 2. 경매 정보 조회 호출 확인
        verify(auctionServiceClient).getAuctionInfo(auctionId);

        // 3. 예외 전파 정책: Repository.save()는 호출되지 않아야 함 (재시도/DLT 처리)
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("admitted 이벤트 정상 처리")
    void shouldProcessAdmittedEventSuccessfully() throws Exception {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        String topic = "notification.queue";
        LocalDateTime occurredAt = LocalDateTime.now();
        LocalDateTime admittedAt = LocalDateTime.now();

        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);
        data.put("admittedAt", admittedAt);

        EventTemplate eventTemplate = EventTemplate.builder()
                .eventId(eventId)
                .eventType("admitted")
                .userId(userId)
                .occurredAt(occurredAt)
                .source("queue-service")
                .data(data)
                .build();

        given(notificationRepository.existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP))
                .willReturn(false);
        given(auctionServiceClient.getAuctionInfo(auctionId))
                .willReturn(null); // 실패해도 진행
        given(objectMapper.writeValueAsString(data))
                .willReturn("{}");
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        eventNotificationService.processEvent(eventTemplate, topic);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("admitted");
        assertThat(saved.getTitle()).isEqualTo("입장 가능");
        assertThat(saved.getMessage()).isEqualTo("지금 입장해 입찰에 참여하세요.");
    }

    @Test
    @DisplayName("outbid 이벤트 정상 처리")
    void shouldProcessOutbidEventSuccessfully() throws Exception {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        String topic = "notification.auction";
        LocalDateTime occurredAt = LocalDateTime.now();
        LocalDateTime outbidAt = LocalDateTime.now();

        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);
        data.put("currentPrice", 50000L);
        data.put("outbidAt", outbidAt);

        EventTemplate eventTemplate = EventTemplate.builder()
                .eventId(eventId)
                .eventType("outbid")
                .userId(userId)
                .occurredAt(occurredAt)
                .source("auction-service")
                .data(data)
                .build();

        given(notificationRepository.existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP))
                .willReturn(false);
        given(auctionServiceClient.getAuctionInfo(auctionId))
                .willReturn(null);
        given(objectMapper.writeValueAsString(data))
                .willReturn("{}");
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        eventNotificationService.processEvent(eventTemplate, topic);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("outbid");
        assertThat(saved.getTitle()).isEqualTo("상회 입찰 발생");
        assertThat(saved.getMessage()).isEqualTo("다른 사용자가 더 높은 금액으로 입찰했습니다.");
    }

    @Test
    @DisplayName("closed 이벤트 정상 처리 (WON)")
    void shouldProcessClosedEventWonSuccessfully() throws Exception {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        String topic = "notification.auction";
        LocalDateTime occurredAt = LocalDateTime.now();
        LocalDateTime closedAt = LocalDateTime.now();

        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);
        data.put("result", "WON");
        data.put("finalPrice", 100000L);
        data.put("closedAt", closedAt);

        EventTemplate eventTemplate = EventTemplate.builder()
                .eventId(eventId)
                .eventType("closed")
                .userId(userId)
                .occurredAt(occurredAt)
                .source("auction-service")
                .data(data)
                .build();

        given(notificationRepository.existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP))
                .willReturn(false);
        given(auctionServiceClient.getAuctionInfo(auctionId))
                .willReturn(null);
        given(objectMapper.writeValueAsString(data))
                .willReturn("{}");
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        eventNotificationService.processEvent(eventTemplate, topic);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("closed");
        assertThat(saved.getTitle()).isEqualTo("경매 종료");
        assertThat(saved.getMessage()).contains("WON");
        assertThat(saved.getMessage()).contains("100000원");
    }

    @Test
    @DisplayName("payment_required 이벤트 정상 처리 (IN_APP + EMAIL)")
    void shouldProcessPaymentRequiredEventSuccessfully() throws Exception {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        String topic = "notification.order";
        LocalDateTime occurredAt = LocalDateTime.now();
        LocalDateTime payDueAt = LocalDateTime.now().plusDays(5);
        String userEmail = "test@example.com";

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("auctionId", auctionId);
        data.put("amount", 100000L);
        data.put("payDueAt", payDueAt);

        EventTemplate eventTemplate = EventTemplate.builder()
                .eventId(eventId)
                .eventType("payment_required")
                .userId(userId)
                .occurredAt(occurredAt)
                .source("order-service")
                .data(data)
                .build();

        // IN_APP 채널 멱등성 체크
        given(notificationRepository.existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP))
                .willReturn(false);
        // EMAIL 채널 멱등성 체크
        given(notificationRepository.existsByEventIdAndChannel(eventId, NotificationChannel.EMAIL))
                .willReturn(false);

        given(auctionServiceClient.getAuctionInfo(auctionId))
                .willReturn(null);
        given(userServiceClient.getUserEmail(userId))
                .willReturn(userEmail);
        given(emailSender.sendHtmlEmail(eq(userEmail), anyString(), anyString(), anyString(), any()))
                .willReturn(true);
        given(objectMapper.writeValueAsString(data))
                .willReturn("{}");
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        eventNotificationService.processEvent(eventTemplate, topic);

        // then
        // IN_APP과 EMAIL 두 채널 모두 저장됨
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        Notification inAppNotification = captor.getAllValues().get(0);
        assertThat(inAppNotification.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(inAppNotification.getTitle()).isEqualTo("결제 필요");
        assertThat(inAppNotification.getMessage()).contains("결제를 완료해주세요");
        assertThat(inAppNotification.getMessage()).contains(payDueAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        Notification emailNotification = captor.getAllValues().get(1);
        assertThat(emailNotification.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(emailNotification.getTitle()).isEqualTo("결제 필요");
        assertThat(emailNotification.getMessage()).contains("결제를 완료해주세요");

        verify(emailSender).sendHtmlEmail(eq(userEmail), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("paid 이벤트 정상 처리 (IN_APP + EMAIL)")
    void shouldProcessPaidEventSuccessfully() throws Exception {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        String topic = "notification.order";
        LocalDateTime occurredAt = LocalDateTime.now();
        LocalDateTime paidAt = LocalDateTime.now();
        String userEmail = "test@example.com";

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("auctionId", auctionId);
        data.put("amount", 100000L);
        data.put("paidAt", paidAt);

        EventTemplate eventTemplate = EventTemplate.builder()
                .eventId(eventId)
                .eventType("paid")
                .userId(userId)
                .occurredAt(occurredAt)
                .source("order-service")
                .data(data)
                .build();

        given(notificationRepository.existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP))
                .willReturn(false);
        given(notificationRepository.existsByEventIdAndChannel(eventId, NotificationChannel.EMAIL))
                .willReturn(false);
        given(auctionServiceClient.getAuctionInfo(auctionId))
                .willReturn(null);
        given(userServiceClient.getUserEmail(userId))
                .willReturn(userEmail);
        given(emailSender.sendHtmlEmail(eq(userEmail), anyString(), anyString(), anyString(), any()))
                .willReturn(true);
        given(objectMapper.writeValueAsString(data))
                .willReturn("{}");
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        eventNotificationService.processEvent(eventTemplate, topic);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        Notification inAppNotification = captor.getAllValues().get(0);
        assertThat(inAppNotification.getType()).isEqualTo("paid");
        assertThat(inAppNotification.getTitle()).isEqualTo("결제 완료");
        assertThat(inAppNotification.getMessage()).isEqualTo("주문이 정상적으로 완료되었습니다.");

        verify(emailSender).sendHtmlEmail(eq(userEmail), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("payment_required 이벤트: EMAIL 발송 실패 시 예외 전파")
    void shouldPropagateExceptionWhenEmailSendFails() throws Exception {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        String topic = "notification.order";
        LocalDateTime occurredAt = LocalDateTime.now();
        LocalDateTime payDueAt = LocalDateTime.now().plusDays(5);
        String userEmail = "test@example.com";

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("auctionId", auctionId);
        data.put("amount", 100000L);
        data.put("payDueAt", payDueAt);

        EventTemplate eventTemplate = EventTemplate.builder()
                .eventId(eventId)
                .eventType("payment_required")
                .userId(userId)
                .occurredAt(occurredAt)
                .source("order-service")
                .data(data)
                .build();

        given(notificationRepository.existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP))
                .willReturn(false);
        given(notificationRepository.existsByEventIdAndChannel(eventId, NotificationChannel.EMAIL))
                .willReturn(false);
        given(auctionServiceClient.getAuctionInfo(auctionId))
                .willReturn(null);
        given(userServiceClient.getUserEmail(userId))
                .willReturn(userEmail);
        given(emailSender.sendHtmlEmail(eq(userEmail), anyString(), anyString(), anyString(), any()))
                .willReturn(false); // EMAIL 발송 실패
        given(objectMapper.writeValueAsString(data))
                .willReturn("{}");
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when & then
        assertThatThrownBy(() -> eventNotificationService.processEvent(eventTemplate, topic))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이메일 발송 실패");

        // IN_APP은 저장되었지만 EMAIL은 실패로 예외 발생
        verify(notificationRepository).save(any(Notification.class));
    }
}

