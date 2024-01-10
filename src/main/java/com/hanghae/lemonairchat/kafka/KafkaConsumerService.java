package com.hanghae.lemonairchat.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	private final ConcurrentHashMap<String, Lock> roomLocks = new ConcurrentHashMap<>();


	public void createOrEnterRoom(String roomId, WebSocketSession webSocketSession) {
		// todo 상문 comment : 어차피 ConcurrentHashMap의 computIfAbsent가 완벽하게 동시성제어를 하고있지 않다면
		//  아래의 로직도 Lock 객체를 새로 생성해서 lock을 거는 행위를 초기 3개의 쓰레드가 동시에 진행한다면
		//  이것 또한 완벽한 동시성 제어가 됐다고 할 수 없지 않나?
		//  redis를 써야하나...
		roomLocks.computeIfAbsent(roomId, key -> new ReentrantLock()).lock();
		try{
			roomSessionListMap.computeIfAbsent(roomId, key -> new ArrayList<>());
			roomSessionListMap.get(roomId).add(webSocketSession);
			log.info("{} 채팅방 새로운 참가자 {} 현재 참가자의 수는 {}",roomId, webSocketSession.getAttributes().get("LoginId"),
				roomSessionListMap.get(roomId).size());
		} finally {
			roomLocks.get(roomId).unlock();
		}
	}
	public void exitRoom(String roomId, WebSocketSession webSocketSession){
		roomLocks.get(roomId).lock();
		try{
			roomSessionListMap.get(roomId).remove(webSocketSession);
			log.info("{} 채팅방에서 {} 가 퇴장 현재 참가자의 수는 {}",roomId, webSocketSession.getAttributes().get("LoginId"),
				roomSessionListMap.get(roomId).size());
		} finally {
			roomLocks.get(roomId).unlock();
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
