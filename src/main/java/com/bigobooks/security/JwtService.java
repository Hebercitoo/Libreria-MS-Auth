package com.bigobooks.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bigobooks.entities.auth.UserAccount;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Emision de tokens JWT (HS512). El token incluye el id y el email del usuario
 * y su rol, de modo que los demas microservicios puedan usarlo mas adelante.
 */
@Component
public class JwtService {

	private final SecretKey key;
	private final String roleClaim;
	private final long expirationMinutes;

	public JwtService(@Value("${jwt.secret}") String secret,
			@Value("${security.jwt.role-claim}") String roleClaim,
			@Value("${jwt.expiration-minutes:120}") long expirationMinutes) {
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 64) {
			throw new IllegalStateException("jwt.secret debe tener al menos 64 caracteres para HS512");
		}
		this.key = Keys.hmacShaKeyFor(bytes);
		this.roleClaim = roleClaim;
		this.expirationMinutes = expirationMinutes;
	}

	public String generateToken(UserAccount user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
		return Jwts.builder()
				.subject(user.getEmail())
				.claim("uid", user.getId())
				.claim(roleClaim, user.getRole() != null ? user.getRole().getName() : null)
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(key, Jwts.SIG.HS512)
				.compact();
	}

	public long getExpirationSeconds() {
		return expirationMinutes * 60;
	}
}
