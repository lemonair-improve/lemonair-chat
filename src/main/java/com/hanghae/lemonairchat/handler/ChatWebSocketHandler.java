package com.hanghae.lemonairchat.handler;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
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
	ReactiveKafkaConsumerTemplate<String, Chat> consumer;

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

		kafkaTopicManager.createTopic(roomId, 3, (short)1).subscribeOn(Schedulers.boundedElastic()).subscribe();

		consumer = kafkaConsumerService.reactiveKafkaConsumerTemplate(roomId);

		consumer.receiveAutoAck()
			.subscribeOn(Schedulers.boundedElastic())
			.map(ConsumerRecord::value)
			.flatMap(chat -> {
				return session.send(Mono.just(session.textMessage(chat.getSender() + ":" + chat.getMessage())))
					.doOnError(throwable -> log.error(" 메세지 전송중 에러 발생 : {}", throwable.getMessage()));
			})
			.doOnError(throwable -> log.error("something bad happened while consuming : {}", throwable.getMessage()))
			.subscribe();

		return session.receive()
			.subscribeOn(Schedulers.boundedElastic())
			.doFinally(signalType -> {
				session.close().subscribe();
				// consumer = null;
			})
			.filter(webSocketMessage -> !webSocketMessage.getPayloadAsText().equals("heartbeat"))
			.flatMap(webSocketMessage -> {
				String message = webSocketMessage.getPayloadAsText();
				log.info("webSocketMessage.getPayloadAsText() : " + webSocketMessage.getPayloadAsText());
				Chat chat = new Chat(message, nickname, roomId);
				chatRepository.save(chat)
					.subscribeOn(Schedulers.boundedElastic())
					.flatMap(savedChat -> reactiveKafkaProducerTemplate.send(roomId, savedChat).then())
					.subscribe();
				return Mono.empty();
			})
			.then();
	}
}