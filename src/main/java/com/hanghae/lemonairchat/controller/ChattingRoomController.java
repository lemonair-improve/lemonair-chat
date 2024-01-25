package com.hanghae.lemonairchat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.hanghae.lemonairchat.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChattingRoomController {
	private final ChatService chatService;

	@PostMapping("/chat/rooms/{roomId}")
	public Mono<ResponseEntity<Boolean>> createChattingRoom(@PathVariable String roomId) {
		log.info("방송 시작으로 채팅방 생성 요청 {}", roomId);
		return chatService.createRoom(roomId).map(ResponseEntity::ok);
	}

	@DeleteMapping("/chat/rooms/{roomId}")
	public Mono<ResponseEntity<Boolean>> removeChattingRoom(@PathVariable String roomId) {
		log.info("방송 종료로 채팅방 삭제 요청 {}", roomId);
		return chatService.removeRoom(roomId).map(ResponseEntity::ok);
	}
}
