package com.hanghae.lemonairchat.kafka;

import com.hanghae.lemonairchat.entity.Chat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;

import jakarta.annotation.PostConstruct;
import reactor.kafka.sender.SenderOptions;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private List<String> bootstrapServers;
    // private String bootstrapServer = "localhost:9091,localhost:9092,localhost:9093";

    @Value("${spring.kafka.producer.key-serializer}")
    private String keySerializer;

    @Value("${spring.kafka.producer.value-serializer}")
    private String valueSerializer;

    @PostConstruct
    public void init(){

        System.out.println(String.join(",", bootstrapServers));
    }

    @Bean
    public ReactiveKafkaProducerTemplate<String, Chat> reactiveKafkaProducerTemplate(
        KafkaProperties properties) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", bootstrapServers));
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);

        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(configProps));
    }

}