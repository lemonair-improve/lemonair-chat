package com.hanghae.lemonairchat.contoller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.hanghae.lemonairchat.entity.Chat;
import com.hanghae.lemonairchat.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Slf4j
@Controller
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

	private final ChatService chatService;
	@GetMapping("/{roomId}")
	public Mono<Integer> getNumOfSubscribers(@PathVariable String roomId) {
		log.info("구독자 조회 하는 roomId : " + roomId);
		Sinks.Many<Chat> sink = chatService.getChatSinkMap().get(roomId);
		return Mono.just(sink.currentSubscriberCount());
	}
}
