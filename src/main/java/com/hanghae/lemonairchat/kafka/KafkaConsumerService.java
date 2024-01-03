package com.hanghae.lemonairchat.kafka;

import com.hanghae.lemonairchat.entity.Chat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;
import reactor.kafka.receiver.ReceiverOptions;

@Service
public class KafkaConsumerService {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServer;

    @Value("${spring.kafka.consumer.key-deserializer}")
    private String keyDeserializer;

    @Value("${spring.kafka.consumer.value-deserializer}")
    private String valueDeserializer;

    public ReactiveKafkaConsumerTemplate<String, Chat> reactiveKafkaConsumerTemplate(String roomId) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "chat-consumer-group" + UUID.randomUUID());
        config.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG,("LemonairHaHaHa"+UUID.randomUUID())); // group instance id를 지정하면 rejoin 안해도 됨
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES,"*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Chat.class);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");


        ReceiverOptions<String, Chat> basicReceiverOptions = ReceiverOptions.create(config);
        ReceiverOptions<String, Chat> options = basicReceiverOptions.subscription(Collections.singletonList(roomId));
//        basicReceiverOptions.subscription(Collections.singletonList(roomId));

//        return new ReactiveKafkaConsumerTemplate<>(basicReceiverOptions);
        return new ReactiveKafkaConsumerTemplate<>(options);
    }
}
