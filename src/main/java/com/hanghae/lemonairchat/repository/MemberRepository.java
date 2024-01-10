package com.hanghae.lemonairchat.repository;


import com.hanghae.lemonairchat.entity.Member;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface MemberRepository extends ReactiveCrudRepository<Member, Long> {

}
