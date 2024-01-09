package com.hanghae.lemonairchat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
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

			private static void setAttributes(WebSocketSession session, JwtTokenSubjectDto dto) {
				session.getAttributes().put("Role", Role.MEMBER.toString());
				session.getAttributes().put("LoginId", dto.getLoginId());
				session.getAttributes().put("Nickname", dto.getNickname());
			}

			@Override
			public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler handler) {
				log.info("연결 요청");
				WebSocketHandler decorator = session -> {
					String path = exchange.getRequest().getURI().getPath();
					String jwtChatAccessToken = path.substring(path.lastIndexOf("/") + 1);
					if (ObjectUtils.isEmpty(jwtChatAccessToken)) {
						return Mono.error(new RuntimeException("chatAccessToken path param이 공백 문자열입니다."));
					}
					if (jwtChatAccessToken.startsWith("VU")) {
						setAttributes(session, JwtTokenSubjectDto.builder()
							.loginId(jwtChatAccessToken)
							.nickname(jwtChatAccessToken)
							.build());
						return handler.handle(session);
					} else if (!"notlogin".equals(jwtChatAccessToken)) {
						log.info("로그인한 사용자의 채팅 웹 소켓 연결 요청");

						setAttributes(session, jwtUtil.getSubjectFromToken(jwtChatAccessToken));
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
