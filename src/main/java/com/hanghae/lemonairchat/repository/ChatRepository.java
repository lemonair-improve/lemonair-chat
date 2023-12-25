package com.hanghae.lemonairchat.repository;

import com.hanghae.lemonairchat.entity.Chat;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ChatRepository extends ReactiveCrudRepository<Chat, Long> {

}
