package com.bidket.order.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 설정
 * - Topic 자동 생성
 * - Consumer Factory 설정 (멱등성 지원)
 * - Error Handler 설정 (DLQ 지원)
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        java.util.Map<String, Object> configs = new java.util.HashMap<>();
        configs.put(org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * Order 서비스가 소비, Auction 서비스가 생산
     * 예: CREATE_ORDER_REQUESTED
     */
    @Bean
    public NewTopic orderAuctionTopic() {
        return TopicBuilder.name("order.auction")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Auction 서비스가 소비, Order 서비스가 생산
     * 예: ORDER_CREATED, ORDER_CREATION_FAILED
     */
    @Bean
    public NewTopic auctionOrderTopic() {
        return TopicBuilder.name("auction.order")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * order.auction 토픽의 DLQ
     */
    @Bean
    public NewTopic orderAuctionDlqTopic() {
        return TopicBuilder.name("order.auction.dlq")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * auction.order 토픽의 DLQ
     */
    @Bean
    public NewTopic auctionOrderDlqTopic() {
        return TopicBuilder.name("auction.order.dlq")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Custom ConsumerFactory with proper JSON deserialization
     * - Trusts all packages for Map<String, Object> deserialization
     * - Uses ErrorHandlingDeserializer to handle deserialization errors gracefully
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Use ErrorHandlingDeserializer to wrap the actual deserializers
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JsonDeserializer specific configuration
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * 멱등성을 지원하는 Kafka Listener Container Factory
     * - 역직렬화 실패 시 DLQ로 전송
     * - 재시도 메커니즘 (최대 3회, 지수 백오프)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> idempotentKafkaListenerContainerFactory(
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Error Handler 설정 - DLQ로 전송
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate, (ConsumerRecord<?, ?> r, Exception e) ->
                        new TopicPartition(r.topic() + ".dlq", r.partition())),
                backoff()
        );

        // 역직렬화 실패 시 재시도하지 않고 즉시 건너뛰기
        errorHandler.addNotRetryableExceptions(DeserializationException.class);
        // 역직렬화 실패 시 seek를 최소화하기 위해 즉시 건너뛰기
        errorHandler.setSeekAfterError(false);

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    /**
     * 지수 백오프 재시도 설정
     * - 최대 3회 재시도
     * - 초기 간격: 1초
     * - 배수: 2.0 (1초 -> 2초 -> 4초)
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
