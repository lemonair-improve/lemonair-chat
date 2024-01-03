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
    private String bootstrapServer = "localhost:9091,localhost:9092,localhost:9093";

    @Value("${spring.kafka.consumer.key-deserializer}")
    private String keyDeserializer;

    @Value("${spring.kafka.consumer.value-deserializer}")
    private String valueDeserializer;

    public ReactiveKafkaConsumerTemplate<String, Chat> reactiveKafkaConsumerTemplate(String roomId) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "chat-consumer-group" + UUID.randomUUID()); // group id 지정
        config.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG,("SchedulerCoordinator"+UUID.randomUUID())); // group instance id를 지정하면 rejoin 안해도 됨
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer); // key deserializer를 정의한다 consume할때 byte array를 객체로 변환해야함
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);// value deserializer를 정의한다 ``
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES,"*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Chat.class);

        // 특정 토픽에 구독된 option을 새로 다른 객체에 할당한 후 사용해야 정상 joining된다.
        ReceiverOptions<String, Chat> basicReceiverOptions = ReceiverOptions.create(config);
        ReceiverOptions<String, Chat> options = basicReceiverOptions.subscription(Collections.singletonList(roomId));
//        basicReceiverOptions.subscription(Collections.singletonList(roomId));
//        return new ReactiveKafkaConsumerTemplate<>(basicReceiverOptions);
        return new ReactiveKafkaConsumerTemplate<>(options);
    }
}
