package com.hanghae.lemonairchat.handler;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.server.ResponseStatusException;

import com.hanghae.lemonairchat.entity.Chat;
import com.hanghae.lemonairchat.kafka.KafkaConsumerService;
import com.hanghae.lemonairchat.kafka.KafkaTopicManager;
import com.hanghae.lemonairchat.repository.ChatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
@Component
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

	private final ChatRepository chatRepository;
	private final KafkaTopicManager kafkaTopicManager;
	private final KafkaConsumerService kafkaConsumerService;
	private final ReactiveKafkaProducerTemplate<String, Chat> reactiveKafkaProducerTemplate;
	private final String PREFIX_ROOMID = "room-";

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		final String role = (String)session.getAttributes().get("Role");
		final String id = (String)session.getAttributes().get("id");
		final String nickname = (String)session.getAttributes().getOrDefault("Nickname", "익명의 사용자");
		final String roomId;

		String getUrl = session.getHandshakeInfo().getUri().getPath();
		String[] pathSegments = getUrl.split("/");

		if (pathSegments.length > 2) {
			roomId = pathSegments[2];
		} else {
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 경로입니다"));
		}

		synchronized (this) {
			kafkaConsumerService.createOrEnterRoom(roomId, session);
		}

		return session.receive()
			.subscribeOn(Schedulers.boundedElastic())
			.doFinally(signalType -> {
				log.info("세션 연결 종료 signalType : {}", signalType.toString());
				session.close().subscribeOn(Schedulers.boundedElastic()).subscribe();
			})
			.filter(webSocketMessage -> !webSocketMessage.getPayloadAsText().equals("heartbeat"))
			.flatMap(webSocketMessage -> {
				String message = webSocketMessage.getPayloadAsText();
				log.info("c->s 메세지 받음 : " + webSocketMessage.getPayloadAsText());
				Chat chat = new Chat(message, nickname, roomId);
				chatRepository.save(chat)
					.subscribeOn(Schedulers.boundedElastic())
					.flatMap(savedChat -> reactiveKafkaProducerTemplate.send("chat", savedChat).then())
					.subscribe();
				return Mono.empty();
			})
			.then();
	}

}