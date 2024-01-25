package com.hanghae.lemonairchat.entity;

import com.hanghae.lemonairchat.constants.MessageType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Chat {
    @Id
    private Long id;
    private String message;
    private String sender;
    private String roomId;
    private String messageType = MessageType.CHAT.toString();
    private String donateMessage = "";
    private LocalDateTime createdAt;

    public Chat(String message, String sender, String roomId) {
        this.message = message;
        this.sender = sender;
        this.roomId = roomId;
        this.createdAt = LocalDateTime.now();
    }

    public Chat(String message, String sender, String roomId, Enum<MessageType> messageType) {
        this.message = message;
        this.sender = sender;
        this.roomId = roomId;
        this.messageType = messageType.toString();
        this.createdAt = LocalDateTime.now();
    }

    public Chat(String message, String sender, String roomId, Enum<MessageType> messageType, String donateMessage) {
        this.message = message;
        this.sender = sender;
        this.roomId = roomId;
        this.messageType = messageType.toString();
        this.donateMessage = donateMessage;
        this.createdAt = LocalDateTime.now();
    }
}

