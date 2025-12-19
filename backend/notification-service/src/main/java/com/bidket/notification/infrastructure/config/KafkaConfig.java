package com.bidket.notification.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;

/**
 * Kafka 설정
 */
@Configuration
public class KafkaConfig {

    /**
     * Kafka Listener Container Factory 설정
     * - 에러 핸들링 및 DLT(Dead Letter Topic) 설정 포함
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // DLT(Dead Letter Topic) 설정
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception ex) -> {
                    // DLT 토픽명: 원본 토픽명 + ".dlt"
                    return new TopicPartition(record.topic() + ".dlt", record.partition());
                }
        );

        // 에러 핸들러 설정 (지수 백오프 재시도)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                backoff()
        );

        // 역직렬화 실패 시 재시도하지 않고 즉시 건너뛰기
        errorHandler.addNotRetryableExceptions(DeserializationException.class);
        // setSeekAfterError는 기본값(true) 유지
        // - true: 에러 발생 시 offset을 현재 위치로 seek하여 재시도
        // - false: 에러 발생 후 offset을 seek하지 않아 수동 커밋 환경에서 중복 처리 위험
        // 수동 커밋 + DLT 구조에서는 기본값(true)이 더 안전함

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

