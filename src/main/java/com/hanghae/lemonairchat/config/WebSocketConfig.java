package com.hanghae.lemonairchat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.server.ServerWebExchange;

import com.hanghae.lemonairchat.constants.Role;
import com.hanghae.lemonairchat.util.JwtTokenSubjectDto;
import com.hanghae.lemonairchat.util.JwtUtil;

import reactor.core.publisher.Mono;

@Configuration
public class WebSocketConfig {

	@Bean
	public WebSocketHandlerAdapter webSocketHandlerAdapter(JwtUtil jwtUtil) {
		return new WebSocketHandlerAdapter(webSocketService(jwtUtil));
	}

	@Bean
	public WebSocketService webSocketService(JwtUtil jwtUtil) {
		HandshakeWebSocketService webSocketService = new HandshakeWebSocketService() {
			@Override
			public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler handler) {
				String jwtAccessToken = exchange.getRequest().getHeaders().getFirst("Authorization");

				String nicknameAttr ;
				final String loginIdAttr;
				Role role;
				if (!ObjectUtils.isEmpty(jwtAccessToken)) {
					JwtTokenSubjectDto jwtTokenSubjectDto = jwtUtil.getSubjectFromToken(jwtAccessToken);
					if (jwtTokenSubjectDto != null) {
						loginIdAttr = jwtTokenSubjectDto.getLoginId();
						nicknameAttr = jwtTokenSubjectDto.getNickname();
						role = Role.MEMBER;
						return exchange.getSession().flatMap(session -> {
							session.getAttributes().put("Role", role.toString());
							session.getAttributes().put("LoginId", loginIdAttr);
							session.getAttributes().put("Nickname", nicknameAttr);
							return super.handleRequest(exchange, handler);
						});
						// TODO: 2023-12-26 방송의 방장 or Manager 인지 파악하는 로직 추가
					}
				}

				role = Role.NOT_LOGIN;
				nicknameAttr = "";
				loginIdAttr = "";

				return exchange.getSession().flatMap(session -> {
					session.getAttributes().put("Role", role.toString());
					session.getAttributes().put("LoginId", loginIdAttr);
					session.getAttributes().put("Nickname", nicknameAttr);
					return super.handleRequest(exchange, handler);
				});

			}
		};

		webSocketService.setSessionAttributePredicate(s -> true);
		return webSocketService;
	}
}
