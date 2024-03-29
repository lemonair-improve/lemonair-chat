package com.hanghae.lemonairchat.util;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class JwtTokenSubjectDto {
	private String loginId;
	private String nickname;

	public JwtTokenSubjectDto(String loginId, String nickname) {
		this.loginId = loginId;
		this.nickname = nickname;
	}
}
