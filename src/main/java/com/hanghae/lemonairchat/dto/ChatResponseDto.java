package com.hanghae.lemonairchat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatResponseDto {
    private String message;
    private String sender;
    private String roomId;
}
