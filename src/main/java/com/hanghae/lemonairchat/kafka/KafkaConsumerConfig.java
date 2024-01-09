package com.hanghae.lemonairchat.kafka;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;

import com.hanghae.lemonairchat.entity.Chat;

import lombok.extern.slf4j.Slf4j;
import reactor.kafka.receiver.ReceiverOptions;

@Configuration
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServer;

    @Value("${spring.kafka.consumer.key-deserializer}")
    private String keyDeserializer;

    @Value("${spring.kafka.consumer.value-deserializer}")
    private String valueDeserializer;

    @Bean
    public ReactiveKafkaConsumerTemplate<String, Chat> reactiveKafkaConsumerTemplate() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "chat-consumer-group" + UUID.randomUUID());
        config.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG,("LemonairHaHaHa"+UUID.randomUUID()));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
      
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES,"*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Chat.class);

        ReceiverOptions<String, Chat> basicReceiverOptions = ReceiverOptions.create(config);
        ReceiverOptions<String, Chat> options = basicReceiverOptions.subscription(Collections.singletonList("chat"));
        return new ReactiveKafkaConsumerTemplate<>(options);
    }
}
