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

@RequiredArgsConstructor
@Component
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {
	private final ChatService chatService;

	@Override
	public Mono<Void> handle(WebSocketSession session) {

		final String role = (String)session.getAttributes().get("Role");
		final String nickname = (String)session.getAttributes().get("Nickname");
		final String roomId;

		String getUrl = session.getHandshakeInfo().getUri().getPath();

		String[] pathSegments = getUrl.split("/");
		if (pathSegments.length > 2) {
			roomId = pathSegments[2];
		} else {
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 경로입니다"));
		}

		log.info("handle getUrl : {}", roomId);

		Flux<Chat> chatFlux = chatService.register(roomId);
		session.receive().flatMap(webSocketMessage -> {
			String message = webSocketMessage.getPayloadAsText();
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

		if (Role.NOT_LOGIN.toString().equals(role)) {
			chatService.sendChat(roomId, new Chat(nickname + "님 채팅방에 오신 것을 환영합니다", "system", roomId));
		}

		return session.send(chatFlux.map(chat -> session.textMessage(chat.getSender() + ": " + chat.getMessage())));
	}
}