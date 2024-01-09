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
public class KafkaConsumerRun implements CommandLineRunner {
	private final ReactiveKafkaConsumerTemplate<String, Chat> reactiveKafkaConsumerTemplate;
	private final Map<String, List<WebSocketSession>> sessionMap = Collections.synchronizedMap(new HashMap<>());

	public void addAndSubscribeTopic(String topic, WebSocketSession webSocketSession) {
		if (sessionMap.containsKey(topic)) {
			// log.info("{} 토픽 은 이미 있음, 이번 참가자는 {}", topic, webSocketSession.getAttributes().get("LoginId"));
			sessionMap.get(topic).add(webSocketSession);
			// log.info("{} 토픽을 구독하는 세션의 수는", sessionMap.get(topic).size());
		} else {
			List<WebSocketSession> webSocketSessionList = new ArrayList<>();
			webSocketSessionList.add(webSocketSession);
			sessionMap.put(topic, webSocketSessionList);
			// log.info("{} 토픽 새로 생성, 첫 참가자는 {}", topic, webSocketSession.getAttributes().get("LoginId"));
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
		String topic = chat.getRoomId();
		String messageToSend = chat.getSender() + ": " + chat.getMessage();
		// log.info("sendToSession, messageToSend : " + messageToSend);
		// log.info("sessionMap.entrySet().size() : " + sessionMap.entrySet().size());
		return Flux.fromIterable(sessionMap.entrySet())
			.filter(entry -> topic.equals(entry.getKey()))
			.flatMap(entry -> Flux.fromIterable(entry.getValue())
				.parallel()
				.runOn(Schedulers.parallel())
				.flatMap(webSocketSession -> {
					// log.info("kafka -> c {} 한테 전송", webSocketSession.getAttributes().get("LoginId").toString());
					return webSocketSession.send(Mono.just(webSocketSession.textMessage(messageToSend)));
				}))
			.then();
	}
}
