package com.hanghae.lemonairchat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.hanghae.lemonairchat.constants.Role;
import com.hanghae.lemonairchat.util.JwtTokenSubjectDto;
import com.hanghae.lemonairchat.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class WebSocketConfig {

	@Bean
	public WebSocketHandlerAdapter webSocketHandlerAdapter(JwtUtil jwtUtil) {
		return new WebSocketHandlerAdapter(webSocketService(jwtUtil));
	}

	@Bean
	public WebSocketService webSocketService(JwtUtil jwtUtil) {
		HandshakeWebSocketService webSocketService = new HandshakeWebSocketService() {

			private static void setAttributes(WebSocketSession session, JwtTokenSubjectDto dto, String roomId) {
				session.getAttributes().put("Role", Role.MEMBER.toString());
				session.getAttributes().put("RoomId", roomId);
				session.getAttributes().put("LoginId", dto.getLoginId());
				session.getAttributes().put("Nickname", dto.getNickname());
			}

			@Override
			public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler handler) {
				WebSocketHandler decorator = session -> {
					String path = exchange.getRequest().getURI().getPath();
					String[] pathSegments = path.split("/");
					if (pathSegments.length < 4) {
						// MappingConfig에서 애초에 pathSegments의 길이가 4인 요청만 여기서 handle하기때문에
						// 여기에 걸릴 일은 없긴 합니다
						return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."));
					}
					String roomId = pathSegments[2];
					String jwtChatAccessToken = pathSegments[3];

					if (jwtChatAccessToken.startsWith("VU")) {
						setAttributes(session, JwtTokenSubjectDto.builder()
							.loginId(jwtChatAccessToken)
							.nickname(jwtChatAccessToken)
							.build(), roomId);
						return handler.handle(session);
					} else if (!"notlogin".equals(jwtChatAccessToken)) {
						log.info("로그인한 사용자의 채팅 웹 소켓 연결 요청");
						setAttributes(session, jwtUtil.getSubjectFromToken(jwtChatAccessToken), roomId);
						return handler.handle(session);

						// TODO: 2023-12-26 방송의 방장 or Manager 인지 파악하는 로직 추가
					} else {
						log.info("로그인하지 않은 사용자의 채팅 웹 소켓 연결 요청");
						session.getAttributes().put("Role", Role.NOT_LOGIN.toString());
						return handler.handle(session);
					}
				};

				return super.handleRequest(exchange, decorator);
			}
		};

		webSocketService.setSessionAttributePredicate(s -> true);
		return webSocketService;
	}
}
