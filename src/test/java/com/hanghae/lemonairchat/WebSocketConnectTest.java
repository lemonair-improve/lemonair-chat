package com.hanghae.lemonairchat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

import reactor.core.publisher.Mono;

public class WebSocketConnectTest {

	@Test
	void test() throws InterruptedException {
		int numberOfConnections = 5; // 동시 접속을 테스트하려는 연결 수

		CountDownLatch latch = new CountDownLatch(numberOfConnections);

		for (int i = 0; i < numberOfConnections; i++) {
			int userId = i + 1;
			connectToWebSocket(userId, latch);
		}
		// 대기, 모든 연결이 완료될 때까지 기다립니다.
		latch.await(10, TimeUnit.SECONDS);
		System.out.println("연결완료");
	}

	private static void connectToWebSocket(int userId, CountDownLatch latch) {
		ReactorNettyWebSocketClient webSocketClient = (new ReactorNettyWebSocketClient());

		String url = "ws://your-websocket-server/chat/room1/VU" + userId;

		webSocketClient.getHttpClient().get().uri(url);


	}
}
