package com.hanghae.lemonairchat.controller;

import com.hanghae.lemonairchat.service.DonationService;
import com.hanghae.lemonairchat.dto.DonationRequestDto;
import com.hanghae.lemonairchat.dto.DonationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class DonationController {
    private final DonationService donationService;

    @PostMapping("/donation/{streamerId}")
    public Mono<ResponseEntity<DonationResponseDto>> donate(
        @RequestBody DonationRequestDto donationRequestDto,
        @PathVariable Long streamerId) {
        return donationService.donate(donationRequestDto, streamerId);
    }
}
