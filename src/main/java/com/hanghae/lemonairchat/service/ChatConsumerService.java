//package com.hanghae.lemonairchat.service;
//
//import com.hanghae.lemonairchat.entity.Chat;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
//import org.springframework.messaging.simp.SimpMessageSendingOperations;
//import org.springframework.stereotype.Service;
//import reactor.core.Disposable;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ChatConsumerService {
//
//    private final ReactiveKafkaConsumerTemplate<String, Chat> kafkaConsumerTemplate;
//    private final SimpMessageSendingOperations messagingTemplate;
//
//
//
//    public Mono<Void> consumeAndSendWebSocketMessages(String roomId) {
//        return kafkaConsumerTemplate.receiveAutoAck()
//            .filter(record -> record.topic().equals(roomId))
//            .map(record -> record.value())
//            .doOnNext(chatMessage -> {
//                log.info("Received message: {}", chatMessage);
//                messagingTemplate.convertAndSend("/topic/" + roomId, chatMessage);
//            })
//            .then();
//    }
//}
