package com.hanghae.lemonairchat.service;

import com.hanghae.lemonairchat.constants.MessageType;
import com.hanghae.lemonairchat.dto.DonationRequestDto;
import com.hanghae.lemonairchat.dto.DonationResponseDto;
import com.hanghae.lemonairchat.entity.Chat;
import com.hanghae.lemonairchat.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonationService {

    private final ReactiveKafkaProducerTemplate<String, Chat> reactiveKafkaProducerTemplate;
    private final MemberRepository memberRepository;

    public Mono<ResponseEntity<DonationResponseDto>> donate(DonationRequestDto donationRequestDto,
        Long streamerId) {
        String donateMessage = donationRequestDto.getNickname() + "님이 " + donationRequestDto.getDonatePoint()
            + "레몬을 후원하셨습니다.";

        return memberRepository.findById(streamerId)
            .flatMap(streamer -> {
                String streamerLoginId = streamer.getLoginId();
                String message = donationRequestDto.getContents();
                Chat donate = new Chat(message, "System", streamerLoginId, MessageType.DONATION, donateMessage);
                return reactiveKafkaProducerTemplate.send("chat", donate)
                    .log()
                    .then(Mono.just(ResponseEntity.ok(new DonationResponseDto(
                        donationRequestDto.getNickname(),
                        streamerId,
                        donationRequestDto.getContents(),
                        donationRequestDto.getDonatePoint()
                    ))));
            });
    }
}
