package com.hanghae.lemonairchat.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	private final ChatRepository chatRepository;

	public Flux<Chat> register(String roomId) {

		log.info("roomId: {}", roomId);
		Sinks.Many<Chat> sink = chatSinkMap.computeIfAbsent(roomId,
			key -> Sinks.many().multicast().onBackpressureBuffer());
		return sink.asFlux();
	}

	public Mono<Boolean> sendChat(String roomId, Chat chat) {
		log.info("roomId: {}, chat: {}", roomId, chat);

		return chatRepository.save(chat).flatMap(savedChat -> {
			Sinks.Many<Chat> sink = chatSinkMap.get(roomId);
			if (sink == null) {
				return Mono.just(false);
			}

			sink.tryEmitNext(savedChat);
			return Mono.just(true);
		});
	}
}