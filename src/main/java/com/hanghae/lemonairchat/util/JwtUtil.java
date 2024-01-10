package com.hanghae.lemonairchat.util;

import java.security.Key;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

	public static final String BEARER_PREFIX = "Bearer ";

	@Value("${jwt.secretKey}")
	private String secretKey;
	private Key key;

	@PostConstruct
	public void init() {
		byte[] bytes = Base64.getDecoder().decode(secretKey);
		key = Keys.hmacShaKeyFor(bytes);
	}

	public JwtTokenSubjectDto getSubjectFromToken(String token) {
		try {
			if (token.startsWith(BEARER_PREFIX)) {
				token = token.substring(BEARER_PREFIX.length());
			}
			var jwtBody = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
			if (!"chatToken".equals(jwtBody.get("type", String.class))) {
				throw new RuntimeException("채팅 서버용 토큰 아님");
			}
			log.info(" chatToken 정상 확인");
			log.info("참가자 id " + jwtBody.get("id", String.class));
			log.info("참가자 nickname " + jwtBody.get("nickname", String.class));

			return new JwtTokenSubjectDto(jwtBody.get("id", String.class), jwtBody.get("nickname", String.class));
		} catch (Exception e) {
			throw new RuntimeException("jwt token parsing 실패");
		}
	}
}
