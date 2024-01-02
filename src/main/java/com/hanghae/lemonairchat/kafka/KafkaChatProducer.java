package com.hanghae.lemonairchat.kafka;

import com.hanghae.lemonairchat.entity.Chat;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaChatProducer {

    private final KafkaTemplate<String, Chat> kafkaTemplate;

    public KafkaChatProducer(KafkaTemplate<String, Chat> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(Chat chat, String roomId) {
        kafkaTemplate.send(roomId, chat);
    }
}