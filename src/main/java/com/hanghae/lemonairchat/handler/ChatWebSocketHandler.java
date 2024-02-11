package com.hanghae.lemonairchat.handler;

import com.hanghae.lemonairchat.entity.Chat;
import com.hanghae.lemonairchat.repository.ChatRepository;
import com.hanghae.lemonairchat.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
@Component
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

	private final ChatRepository chatRepository;
	private final ChatService chatService;
	private final ReactiveKafkaProducerTemplate<String, Chat> reactiveKafkaProducerTemplate;

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		final String nickname = (String)session.getAttributes().get("Nickname");
		final String roomId = (String)session.getAttributes().get("RoomId");
		if (roomId == null) {
			throw new RuntimeException("비정상 요청 세션의 roomId가 없음 " + session.getId());
		}

		if (nickname == null) {
			throw new RuntimeException("nickname이 없음");
		}

		chatService.enterRoom(roomId, session);

		return session.receive()
			.subscribeOn(Schedulers.boundedElastic())
			.filter(this::filterHeartBeat)
			.map(webSocketMessage -> new Chat(webSocketMessage.getPayloadAsText(), nickname, roomId))
			.flatMap(chat -> Mono.when(chatRepository.save(chat), reactiveKafkaProducerTemplate.send("chat", chat))
				.doOnError(exception -> log.error("채팅 전송중 오류 발생 : " + exception.getMessage())))
			.doFinally(signalType -> {
				Mono.when(chatService.exitRoom(roomId, session), session.close())
					.subscribeOn(Schedulers.boundedElastic())
					.subscribe();
			})
			.then();
	}

	private boolean filterHeartBeat(WebSocketMessage webSocketMessage) {
		return !webSocketMessage.getPayloadAsText().equals("heartbeat");
	}
}