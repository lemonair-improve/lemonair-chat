package com.hanghae.lemonairchat.kafka;

import com.hanghae.lemonairchat.entity.Chat;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Service;

@Service
public class KafkaChatConsumer {

    private final Properties properties;
    private final String groupId = "chat-consumer-group" + UUID.randomUUID();

    public KafkaChatConsumer() {
        properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9091,localhost:9092,localhost:9093");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    }

    public KafkaConsumer<String, Chat> createConsumer(String roomId) {
//        properties.put(ConsumerConfig.GROUP_ID_CONFIG, roomId + "-" + nickname + UUID.randomUUID());
        KafkaConsumer<String, Chat> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singletonList(roomId));
        return consumer;
    }


}
