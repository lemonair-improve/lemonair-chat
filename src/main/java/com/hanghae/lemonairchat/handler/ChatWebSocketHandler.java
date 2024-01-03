package com.hanghae.lemonairchat.handler;

import com.hanghae.lemonairchat.entity.Chat;
import com.hanghae.lemonairchat.kafka.KafkaConsumerService;
import com.hanghae.lemonairchat.kafka.KafkaTopicManager;
import com.hanghae.lemonairchat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
@Component
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {
	private final ChatRepository chatRepository;
	private final KafkaTopicManager kafkaTopicManager;
	private final KafkaConsumerService kafkaConsumerService;
	private final ReactiveKafkaProducerTemplate<String, Chat> reactiveKafkaProducerTemplate;



	@Override
	public Mono<Void> handle(WebSocketSession session) {
		final String role = (String)session.getAttributes().get("Role");
		final String nickname = (String)session.getAttributes().getOrDefault("Nickname", "익명의 사용자");
		final String roomId;
		// final String
		String getUrl = session.getHandshakeInfo().getUri().getPath();

		String[] pathSegments = getUrl.split("/");

		if (pathSegments.length > 2) {
			roomId = pathSegments[2];
		} else {
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 경로입니다"));
		}

		kafkaTopicManager.createTopic(roomId, 3, (short) 1);
		ReactiveKafkaConsumerTemplate<String, Chat> consumer = kafkaConsumerService.reactiveKafkaConsumerTemplate(roomId);
//		log.info("consumer: {}", consumer);

		consumer.receiveAutoAck()
			.map(ConsumerRecord::value)
			.flatMap(chat -> {
//				log.info("successfully consumed {}={}", Chat.class.getSimpleName(), chat);
				return session.send(Mono.just(session.textMessage(chat.getSender() + ":" + chat.getMessage())))
					.log()
					.doOnError(throwable -> log.error(" 메세지 전송중 에러 발생 : {}", throwable.getMessage()));
			})
			.doOnError(throwable -> log.error("something bad happened while consuming : {}", throwable.getMessage())).subscribe();

		return session.receive()
			.flatMap(webSocketMessage -> {
				String message = webSocketMessage.getPayloadAsText();
//				log.info("Received message: {}", message);
				Chat chat = new Chat(message, nickname, roomId);
				return chatRepository.save(chat)
					.flatMap(savedChat -> reactiveKafkaProducerTemplate.send(roomId, savedChat).then());
			})
			.then();
	}
}