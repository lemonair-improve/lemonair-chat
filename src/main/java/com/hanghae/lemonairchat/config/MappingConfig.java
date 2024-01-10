package com.hanghae.lemonairchat.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import com.hanghae.lemonairchat.handler.ChatWebSocketHandler;

@Configuration
public class MappingConfig {
	@Bean
	SimpleUrlHandlerMapping simpleUrlHandlerMapping(
		ChatWebSocketHandler chatWebSocketHandler
	) {
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		Map<String, WebSocketHandler> urlMap = new HashMap<>();

		urlMap.put("/chat/{roomId}/{chatToken}", chatWebSocketHandler);
		mapping.setOrder(1);
		mapping.setUrlMap(urlMap);

		return mapping;
	}
}