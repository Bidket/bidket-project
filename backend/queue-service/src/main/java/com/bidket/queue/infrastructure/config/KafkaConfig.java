package com.bidket.queue.infrastructure.config;

import com.bidket.queue.domain.event.NotificationEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

@Configuration
public class KafkaConfig {

    @Value("${kafka.queue.enter.notification.topic}")
    private String enterNotificationTopic;
    @Value("${kafka.queue.enter.notification.partition}")
    private int enterNotificationPartition;
    @Value("${kafka.queue.enter.notification.replicas}")
    private int enterNotificationReplicas;
    @Value("${kafka.queue.enter.notification.retention}")
    private String enterNotificationRetention;

    @Bean
    public ReactiveKafkaProducerTemplate<String, NotificationEvent> reactiveKafkaProducerTemplate(
            KafkaProperties properties,
            SslBundles sslBundles) {
        return new ReactiveKafkaProducerTemplate<>(
                SenderOptions.create(properties.buildProducerProperties(sslBundles))
        );
    }

    @Bean
    public NewTopic queueEnterTopic() {
        return TopicBuilder.name(enterNotificationTopic)
                .partitions(enterNotificationPartition)
                .replicas(enterNotificationReplicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, enterNotificationRetention)
                .build();
    }
}
