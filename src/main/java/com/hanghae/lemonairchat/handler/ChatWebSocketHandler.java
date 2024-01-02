package com.hanghae.lemonairchat.handler;

import com.hanghae.lemonairchat.entity.Chat;
import com.hanghae.lemonairchat.kafka.KafkaTopicManager;
import com.hanghae.lemonairchat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
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

		return session.receive()
			.flatMap(webSocketMessage -> {
				String message = webSocketMessage.getPayloadAsText();
				log.info("Received message: {}", message);
				Chat chat = new Chat(message, nickname, roomId);
				return chatRepository.save(chat)
					.flatMap(savedChat -> reactiveKafkaProducerTemplate.send(roomId, savedChat).then());
			})
			.then();

//		return chatConsumerService.consumeAndSendWebSocketMessages(roomId);
	}

//	private Mono<Void> doWebSocketLogic(WebSocketSession session, String roomId) {
//		KafkaConsumer<String, Chat> consumer = kafkaChatConsumer.createConsumer(roomId);
//
//		ConsumerRecords<String, Chat> records = consumer.poll();
//
//		for (ConsumerRecord<String, Chat> record : records) {
//			// 메시지 처리
//			Chat chatMessage = record.value();
//			// 받은 채팅 메시지를 처리하는 로직을 여기에 추가하세요
//			System.out.println("Received message: " + chatMessage);
//			return session.send(session.textMessage(chatMessage.getSender()) + ": " + chatMessage.getMessage());
//		}
//		return Mono.empty();
//	}

//	public Mono<Void> sendToKafka(WebSocketSession session, String roomId, String nickname) {
//		log.info("sendToKafka");
//
//		return session.receive()
//			.flatMap(webSocketMessage -> {
//				String message = webSocketMessage.getPayloadAsText();
//				log.info("message : {}", message);
//				Chat chat = new Chat(message, nickname, roomId);
//				return chatRepository.save(chat)
//					.flatMap(savedChat -> Mono.fromRunnable(() -> kafkaTemplate.send(roomId, savedChat)))
//					.then();
//			})
//			.then();
//	}
}