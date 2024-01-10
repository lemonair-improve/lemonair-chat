package com.hanghae.lemonairchat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonationRequestDto {
    private String nickname;
    private Long streamerId;
    private String contents;
    private int donatePoint;

    public DonationRequestDto(String nickname, Long streamerId, String contents, int donatePoint) {
        this.nickname = nickname;
        this.streamerId = streamerId;
        this.contents = contents;
        this.donatePoint = donatePoint;
    }
}
