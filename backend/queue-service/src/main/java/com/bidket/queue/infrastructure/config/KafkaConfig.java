package com.bidket.queue.infrastructure.config;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;

@Configuration
public class KafkaConfig {
    @Bean
    public ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate(
            KafkaProperties properties,
            SslBundles sslBundles) {
        return new ReactiveKafkaProducerTemplate<>(
                SenderOptions.create(properties.buildProducerProperties(sslBundles))
        );
    }
}
