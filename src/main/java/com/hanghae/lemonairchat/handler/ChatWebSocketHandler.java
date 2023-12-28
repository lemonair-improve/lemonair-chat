package com.hanghae.lemonairchat.handler;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.server.ResponseStatusException;

import com.hanghae.lemonairchat.constants.Role;
import com.hanghae.lemonairchat.entity.Chat;
import com.hanghae.lemonairchat.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@RequiredArgsConstructor
@Component
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {
	private final ChatService chatService;

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

		log.info("handle roomId : {}", roomId);

		Flux<Chat> chatFlux = chatService.register(roomId);
		session.receive().doFinally(signalType -> {
			// WebSocket 연결이 종료될 때의 로직
			if (signalType == SignalType.ON_COMPLETE || signalType == SignalType.CANCEL) {
				log.info("WebSocket 연결이 종료되었습니다.");
				session.close().log().subscribe();
				chatService.deRegister(roomId);
			}
		}).flatMap(webSocketMessage -> {
			String message = webSocketMessage.getPayloadAsText();
			log.info("webSocketMessage.getNativeMessage() : " + webSocketMessage.getNativeMessage());
			log.info("webSocketMessage.getType() : " + webSocketMessage.getType());
			log.info("webSocketMessage.getPayloadAsText() : " + webSocketMessage.getPayloadAsText());
			if (Role.NOT_LOGIN.toString().equals(role)) {
				return Mono.just(true);
			}
			return chatService.sendChat(roomId, new Chat(message, nickname, roomId)).flatMap(result -> {
				if (!result) {
					return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"));
				}
				return Mono.just(true);
			});
		}).subscribe();

		if (!Role.NOT_LOGIN.toString().equals(role)) {
			chatService.sendChat(roomId, new Chat(nickname + "님 채팅방에 오신 것을 환영합니다", "system", roomId));
		}

		return session.send(chatFlux.map(chat -> session.textMessage(chat.getSender() + ": " + chat.getMessage())));
	}

}