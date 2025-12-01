package com.bidket.auction.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

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

    @Bean
    public NewTopic sagaCompensationTopic() {
        return TopicBuilder.name("saga.compensation")
            .partitions(3)
            .replicas(1)
            .build();
    }
}

