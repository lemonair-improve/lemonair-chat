package com.hanghae.lemonairchat.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
public class KafkaConsumerService implements CommandLineRunner {
	private final ReactiveKafkaConsumerTemplate<String, Chat> reactiveKafkaConsumerTemplate;
	private final Map<String, List<WebSocketSession>> roomSessionListMap = new ConcurrentHashMap<>();
	// private final ConcurrentHashMap<String, Lock> roomLocks = new ConcurrentHashMap<>();

	public void createOrEnterRoom(String roomId, WebSocketSession webSocketSession) {
		roomSessionListMap.putIfAbsent(roomId, new ArrayList<>());

		List<WebSocketSession> webSocketSessionList = roomSessionListMap.get(roomId);
		synchronized (webSocketSessionList) {
			webSocketSessionList.add(webSocketSession);
		}

		log.info("{} 채팅방 새로운 참가자 {} 현재 참가자의 수는 {}", roomId, webSocketSession.getAttributes().get("LoginId"),
			roomSessionListMap.get(roomId).size());
	}

	public void exitRoom(String roomId, WebSocketSession webSocketSession) {
		List<WebSocketSession> webSocketSessionList = roomSessionListMap.get(roomId);
		synchronized (webSocketSessionList) {
			webSocketSessionList.remove(webSocketSession);
		}
		log.info("{} 채팅방에서 {} 가 퇴장 현재 참가자의 수는 {}", roomId, webSocketSession.getAttributes().get("LoginId"),
			roomSessionListMap.get(roomId).size());
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
		String messageToSend = chat.getSender() + ": " + chat.getMessage();
		log.info("sendToSession, messageToSend : " + messageToSend);
		log.info("sessionMap.entrySet().size() : " + roomSessionListMap.entrySet().size());
		return Flux.fromIterable(roomSessionListMap.entrySet())
			.filter(entry -> roomId.equals(entry.getKey()))
			.flatMap(entry -> Flux.fromIterable(entry.getValue())
				// .parallel()
				// .runOn(Schedulers.parallel())
				.flatMap(webSocketSession -> {
					log.info("kafka -> c {} 한테 전송", webSocketSession.getAttributes().get("LoginId").toString());
					return webSocketSession.send(Mono.just(webSocketSession.textMessage(messageToSend)));
				}))
			.then();
	}
}
