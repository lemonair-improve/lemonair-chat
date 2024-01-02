package com.hanghae.lemonairchat.service;

import com.hanghae.lemonairchat.kafka.KafkaChatConsumer;
import com.hanghae.lemonairchat.kafka.KafkaChatProducer;
import com.hanghae.lemonairchat.kafka.KafkaTopicManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.hanghae.lemonairchat.entity.Chat;
import com.hanghae.lemonairchat.repository.ChatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
	private static Map<String, Sinks.Many<Chat>> chatSinkMap = new ConcurrentHashMap<>();
	private final KafkaTopicManager kafkaTopicManager;
	private final KafkaChatProducer kafkaChatProducer;
	private final KafkaChatConsumer kafkaChatConsumer;
	private final KafkaTemplate<String, Chat> kafkaTemplate;

	private final ChatRepository chatRepository;

	public Flux<Chat> register(String roomId) {

		Sinks.Many<Chat> sink = chatSinkMap.computeIfAbsent(roomId,
			key -> Sinks.many().multicast().onBackpressureBuffer());
		log.info("현재 구독자 수: {}", sink.currentSubscriberCount());
		return sink.asFlux();
	}

	public Mono<Boolean> sendChat(String roomId, Chat chat) {
		log.info("roomId: {}, sender: {} chatMessage: {}", roomId, chat.getSender(), chat.getMessage());

		return chatRepository.save(chat).flatMap(savedChat -> {
			Sinks.Many<Chat> sink = chatSinkMap.get(roomId);
			if (sink == null) {
				return Mono.just(false);
			}

			sink.tryEmitNext(savedChat);
			return Mono.just(true);
		});
	}

	Mono<Void> sendToKafka(String roomId, Chat chat) {
		return chatRepository.save(chat)
			.flatMap(savedChat -> Mono.fromRunnable(() -> kafkaTemplate.send("chat-" + roomId, savedChat)))
			.then();
	}

	public void deRegister(String roomId) {
		// log.info("deRegister : roomId : {}, 현재 구독자 수 {}",roomId, chatSinkMap.get(roomId).currentSubscriberCount());
		chatSinkMap.computeIfPresent(roomId, (key, value) -> value.currentSubscriberCount() <=1 ? null : value);
	}
}