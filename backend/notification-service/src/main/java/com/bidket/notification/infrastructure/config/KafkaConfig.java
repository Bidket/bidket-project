package com.bidket.notification.infrastructure.config;

import com.bidket.notification.domain.model.EventTemplate;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.messaging.converter.MessageConversionException;

/**
 * Kafka 설정
 */
@Configuration
public class KafkaConfig {

    /**
     * Kafka Listener Container Factory 설정
     * - 에러 핸들링 및 DLT(Dead Letter Topic) 설정 포함
     * - AckMode: MANUAL (수동 커밋)
     * - 제네릭: EventTemplate로 타입 안정성 확보
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventTemplate> kafkaListenerContainerFactory(
            ConsumerFactory<String, EventTemplate> consumerFactory,
            KafkaTemplate<String, EventTemplate> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, EventTemplate> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // AckMode 명시: MANUAL (수동 커밋)
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // DLT(Dead Letter Topic) 설정
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception ex) -> {
                    // DLT 토픽명: 원본 토픽명 + ".dlt"
                    // 파티션 전략: 원본 파티션 유지 (원본 토픽과 동일한 파티션 수 필요)
                    // 주의: DLT 토픽의 파티션 수가 원본 토픽보다 적으면 publish 실패 가능
                    // 안전한 대안: partition 0으로 고정 (return new TopicPartition(record.topic() + ".dlt", 0);)
                    return new TopicPartition(record.topic() + ".dlt", record.partition());
                }
        );

        // 에러 핸들러 설정 (지수 백오프 재시도)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                backoff()
        );

        // 재시도하지 않을 예외: 역직렬화/컨버전 실패, 잘못된 요청 (DLT로 전송)
        // poison 메시지가 재시도 반복되는 것을 방지하기 위해 모든 역직렬화 관련 예외 포함
        // 참고: RecordDeserializationException은 Kafka 클라이언트 2.5.0+ 에서만 사용 가능
        // 현재 버전에서는 SerializationException으로 대부분의 역직렬화 실패를 커버
        errorHandler.addNotRetryableExceptions(
                DeserializationException.class,           // Spring Kafka 역직렬화 실패
                MessageConversionException.class,         // 메시지 컨버전 실패
                SerializationException.class,             // Kafka 직렬화/역직렬화 실패 (org.apache.kafka.common.errors)
                IllegalArgumentException.class             // 잘못된 요청 (eventType null, payload null 등)
        );
        // setSeekAfterError는 기본값(true) 유지
        // - true: 에러 발생 시 offset을 현재 위치로 seek하여 재시도
        // - false: 에러 발생 후 offset을 seek하지 않아 수동 커밋 환경에서 중복 처리 위험
        // 수동 커밋 + DLT 구조에서는 기본값(true)이 더 안전함
        // 역직렬화/컨버전 실패 및 IllegalArgumentException은 재시도하지 않고 즉시 DLT로 전송

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    /**
     * 지수 백오프 설정
     * - 최대 3회 재시도
     * - 초기 간격: 1초
     * - 배수: 2.0
     * - 최대 간격: 10초
     */
    private ExponentialBackOffWithMaxRetries backoff() {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);
        return backOff;
    }
}

