package com.hanghae.lemonairchat.kafka;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private final Map<String, List<WebSocketSession>> roomSessionListMap = Collections.synchronizedMap(new HashMap<>());

	public void createOrJoinRoom(String roomId, WebSocketSession webSocketSession) {
		if (roomSessionListMap.containsKey(roomId)) {
			log.info("{} 토픽 은 이미 있음, 이번 참가자는 {}", roomId, webSocketSession.getAttributes().get("LoginId"));
			roomSessionListMap.get(roomId).add(webSocketSession);
			log.info("토픽을 구독하는 세션의 수 : {}", roomSessionListMap.get(roomId).size());
		} else {
			List<WebSocketSession> webSocketSessionList = new ArrayList<>();
			webSocketSessionList.add(webSocketSession);
			roomSessionListMap.put(roomId, webSocketSessionList);
			log.info("{} 토픽 새로 생성, 첫 참가자는 {}", roomId, webSocketSession.getAttributes().get("LoginId"));
		}
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
				.parallel()
				.runOn(Schedulers.parallel())
				.flatMap(webSocketSession -> {
					log.info("kafka -> c {} 한테 전송", webSocketSession.getAttributes().get("LoginId").toString());
					return webSocketSession.send(Mono.just(webSocketSession.textMessage(messageToSend)));
				}))
			.then();
	}
}
