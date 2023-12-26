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

	public String substringToken(String tokenValue) {
		if (StringUtils.hasText(tokenValue) && tokenValue.startsWith(BEARER_PREFIX)) {
			return tokenValue.substring(7);
		}
		throw new NullPointerException("Not Found Token");
	}

	public Mono<Boolean> validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return Mono.just(true);
		} catch (SecurityException | MalformedJwtException | SignatureException e) {
			log.error("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
		} catch (ExpiredJwtException e) {
			log.error("Expired JWT token, 만료된 JWT token 입니다.");
			return Mono.just(false);
		} catch (UnsupportedJwtException e) {
			log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
		} catch (IllegalArgumentException e) {
			log.error("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
		}
		return Mono.just(false);
	}

	public JwtTokenSubjectDto getSubjectFromToken(String token) {
		try {
			if (token.startsWith("Bearer ")) {
				token = token.substring("Bearer ".length());
			}
			var jwtBody = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
			log.info(jwtBody.get("id", String.class));
			log.info(jwtBody.get("nickname", String.class));
			return new JwtTokenSubjectDto(jwtBody.get("id", String.class), jwtBody.get("nickname", String.class));
		} catch (Exception e) {
			log.error(e.toString());
			return null;
		}
	}
}
