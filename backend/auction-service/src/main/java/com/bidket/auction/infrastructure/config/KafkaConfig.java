package com.bidket.auction.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${auction.kafka.topics.auction-events:auction.events}")
    private String auctionEventsTopicName;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        java.util.Map<String, Object> configs = new java.util.HashMap<>();
        configs.put(org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic auctionEventsTopic() {
        return TopicBuilder.name("auction.events")
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic bidEventsTopic() {
        return TopicBuilder.name("bid.events")
            .partitions(3)
            .replicas(1)
            .build();
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

    @Bean
    public NewTopic auctionOrderDlqTopic() {
        return TopicBuilder.name("auction.order.dlq")
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic orderAuctionDlqTopic() {
        return TopicBuilder.name("order.auction.dlq")
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic auctionEventsDlqTopic() {
        return TopicBuilder.name(auctionEventsTopicName + ".dlq")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> idempotentKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {
        // ConsumerFactory는 Spring Boot가 자동으로 생성하는 빈을 주입받음
        // ConcurrentKafkaListenerContainerFactory를 새로 만들 때는 반드시 ConsumerFactory가 필요함
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
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

    private ExponentialBackOffWithMaxRetries backoff() {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);
        return backOff;
    }
}

