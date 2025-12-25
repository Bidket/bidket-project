package com.bidket.queue.infrastructure.config;

import com.bidket.queue.domain.event.EventTemplate;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${kafka.notification.queue.admitted.topic}")
    private String enterNotificationTopic;
    @Value("${kafka.notification.queue.admitted.partition}")
    private int enterNotificationPartition;
    @Value("${kafka.notification.queue.admitted.replicas}")
    private int enterNotificationReplicas;
    @Value("${kafka.notification.queue.admitted.retention}")
    private String enterNotificationRetention;

    @Value("${kafka.notification.queue.near_turn.topic}")
    private String nearTurnNotificationTopic;
    @Value("${kafka.notification.queue.near_turn.partition}")
    private int nearTurnNotificationPartition;
    @Value("${kafka.notification.queue.near_turn.replicas}")
    private int nearTurnNotificationReplicas;
    @Value("${kafka.notification.queue.near_turn.retention}")
    private String nearTurnNotificationRetention;

    @Bean
    public SenderOptions<String, EventTemplate> reactiveKafkaProducerTemplate(
            KafkaProperties properties,
            SslBundles sslBundles) {
        return SenderOptions.create(properties.buildProducerProperties(sslBundles));
    }

    @Bean
    public KafkaSender<String, EventTemplate> kafkaSender(SenderOptions<String, EventTemplate> senderOptions) {
        return KafkaSender.create(senderOptions);
    }

    @Bean
    public ReceiverOptions<String, EventTemplate> receiverOptions(
            KafkaProperties properties,
            SslBundles sslBundles
    ) {
        Map<String, Object> consumerProperties = properties.buildConsumerProperties(sslBundles);

        JsonDeserializer<EventTemplate> jsonDeserializer = new JsonDeserializer<>(EventTemplate.class);
        jsonDeserializer.setRemoveTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeMapperForKey(true);

        return ReceiverOptions.<String, EventTemplate>create(consumerProperties)
                .withValueDeserializer(jsonDeserializer);
    }

    @Bean
    public NewTopic queueEnterTopic() {
        return TopicBuilder.name(enterNotificationTopic)
                .partitions(enterNotificationPartition)
                .replicas(enterNotificationReplicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, enterNotificationRetention)
                .build();
    }

    @Bean
    public NewTopic queueNearTurnTopic() {
        return TopicBuilder.name(nearTurnNotificationTopic)
                .partitions(nearTurnNotificationPartition)
                .replicas(nearTurnNotificationReplicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, nearTurnNotificationRetention)
                .build();
    }
}
