package com.hanghae.lemonairchat.handler;

import java.util.function.Function;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
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
			// .filter(chat -> !chat.getMessage().equals("heartbeat"))
			.flatMap(chat -> {
				return session.send(Mono.just(session.textMessage(chat.getSender() + ":" + chat.getMessage())))
					.doOnError(throwable -> log.error(" 메세지 전송중 에러 발생 : {}", throwable.getMessage()));
			})
			.doOnError(throwable -> log.error("something bad happened while consuming : {}", throwable.getMessage()))
			.subscribe();

		// log.info("consumer: {}", consumer);

		return session.receive().subscribeOn(Schedulers.boundedElastic()).doFinally(signalType -> {
			session.close().subscribe();
			consumer = null;
		}).flatMap(webSocketMessage -> {
			String message = webSocketMessage.getPayloadAsText();
			log.info("webSocketMessage.getType() : " + webSocketMessage.getType());
			log.info("webSocketMessage.getPayloadAsText() : " + webSocketMessage.getPayloadAsText());
			if (isPingMessage(webSocketMessage)) {
				// 클라이언트로부터의 ping 메시지에 대한 처리
				log.info("ping 메세지를 받아서 pong을 리턴하기");
				// return session.send(Mono.just(session.pongMessage(payloadFactory(session.bufferFactory())))).log();
				return session.send(Mono.just(session.textMessage("pong"))).log();

			} else {
				Chat chat = new Chat(message, nickname, roomId);
				chatRepository.save(chat)
					.subscribeOn(Schedulers.boundedElastic())
					.flatMap(savedChat -> reactiveKafkaProducerTemplate.send(roomId, savedChat).then())
					.subscribe();
			}
			return Mono.empty();
		}).then();
	}

	private Function<DataBufferFactory, DataBuffer> payloadFactory(DataBufferFactory bufferFactory) {
		// 원하는 페이로드 생성 로직 구현
		// 예: "Pong" 문자열을 포함하는 DataBuffer 생성
		return factory -> {
			byte[] payloadBytes = "Pong".getBytes();
			DataBuffer dataBuffer = bufferFactory.allocateBuffer(payloadBytes.length);
			dataBuffer.write(payloadBytes);
			return dataBuffer;
		};
	}

	// 메시지가 ping 메시지인지 확인하는 메서드
	private boolean isPingMessage(WebSocketMessage webSocketMessage) {
		String message = webSocketMessage.getPayloadAsText();
		return "ping".equals(message);
	}

	private boolean isK6PingMessage(WebSocketMessage webSocketMessage){
		return webSocketMessage.getType().equals(WebSocketMessage.Type.PING);
	}
}