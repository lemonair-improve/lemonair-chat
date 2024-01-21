package com.hanghae.lemonairchat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.hanghae.lemonairchat.entity.Chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatService implements CommandLineRunner {
	private final ReactiveKafkaConsumerTemplate<String, Chat> reactiveKafkaConsumerTemplate;
	private final Map<String, List<WebSocketSession>> rooms = new ConcurrentHashMap<>();

	public void enterRoom(String roomId, WebSocketSession webSocketSession) {
		Mono.just(roomId)
			.flatMap(room -> {
				rooms.putIfAbsent(room, new CopyOnWriteArrayList<>());
				return Mono.just(rooms.get(room));
			})
			.flatMap(sessionsInRoom -> {
				sessionsInRoom.add(webSocketSession);
				return Mono.just(sessionsInRoom);
			})
			.doOnNext(sessionsInRoom -> log.info("{} 채팅방 새로운 참가자 {} 현재 참가자의 수는 {}", roomId,
				webSocketSession.getAttributes().get("LoginId"), sessionsInRoom.size()))
			.subscribe();
	}

	public Mono<Void> exitRoom(String roomId, WebSocketSession webSocketSession) {
		return Mono.just(roomId)
			.filter(rooms::containsKey)
			.switchIfEmpty(Mono.error(new RuntimeException("채팅방 퇴장 예외 발생 : 채팅방이 존재하지 않음")))
			.map(rooms::get)
			.flatMap(sessionsInRoom -> Mono.fromRunnable(() -> sessionsInRoom.remove(webSocketSession)))
			.doOnSuccess(removed -> log.info("{} 채팅방에서 {} 가 퇴장 현재 참가자의 수는 {}", roomId, webSocketSession.getAttributes().get("LoginId"), rooms.get(roomId).size()))
			.then();
	}

	@Override
	public void run(String... args) {
		reactiveKafkaConsumerTemplate.receiveAutoAck()
			.subscribeOn(Schedulers.boundedElastic())
			.map(ConsumerRecord::value)
			.flatMap(this::sendToSession)
			.doOnError(throwable -> log.error("something bad happened while consuming : {}", throwable.getMessage()))
			.subscribe();
	}

	public Mono<Void> sendToSession(Chat chat) {
		String roomId = chat.getRoomId();
		String messageToSend = chat.getMessageType() + ":" + chat.getSender() + ":" + chat.getDonateMessage() + ":" + chat.getMessage();
		log.info("sendToSession, messageToSend : " + messageToSend);
		log.info("sessionMap.entrySet().size() : " + rooms.entrySet().size());
		return Flux.fromIterable(rooms.entrySet())
			.filter(entry -> roomId.equals(entry.getKey()))
			.flatMap(entry -> Flux.fromIterable(entry.getValue())
				.parallel()
				.runOn(Schedulers.parallel())
				.flatMap(webSocketSession -> {
					log.info("kafka -> c {} 한테 전송", webSocketSession.getAttributes().get("LoginId").toString());
					return webSocketSession.send(Mono.just(webSocketSession.textMessage(messageToSend)));
				}))
			.then();
	}

	public Mono<Boolean> createRoom(String roomId) {
		return Mono.just(roomId)
			.filter(room -> !rooms.containsKey(room))
			.switchIfEmpty(Mono.error(new RuntimeException(roomId + " 채팅방은 이미 개설되어있음")))
			.flatMap(createRoom -> {
				rooms.put(createRoom, new CopyOnWriteArrayList<>());
				return Mono.just(true);
			});
	}

	public Mono<Boolean> removeRoom(String roomId) {
		return Mono.defer(() -> Mono.just(roomId)
			.filter(rooms::containsKey)
			.switchIfEmpty(Mono.error(new RuntimeException(roomId + " 채팅방이 없는데 삭제 요청")))
			.map(existingRoomId -> {
				rooms.remove(existingRoomId);
				return true;
			}));
	}
}
