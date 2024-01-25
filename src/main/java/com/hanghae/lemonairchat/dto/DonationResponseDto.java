package com.hanghae.lemonairchat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonationResponseDto {
    private String nickname;
    private Long streamerId;
    private String contents;
    private int donatePoint;

    public DonationResponseDto(String nickname, Long streamerId, String contents,
        int donatePoint) {
        this.nickname = nickname;
        this.streamerId = streamerId;
        this.contents = contents;
        this.donatePoint = donatePoint;
    }

}
