package com.cherry.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final long ACCESS_TOKEN_VALIDITY_MS = 60 * 60 * 1000L;
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${jwt.secret:cherry-dev-secret-please-change-32bytes}")
    private String secret;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        secretKey = Keys.hmacShaKeyFor(padSecret(secret).getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ACCESS_TOKEN_VALIDITY_MS)))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        Object value = parseClaims(token).get("userId");
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String padSecret(String value) {
        if (value == null) {
            return "cherry-dev-secret-please-change-32bytes";
        }
        String secretValue = value.trim();
        if (secretValue.length() >= MIN_SECRET_LENGTH) {
            return secretValue;
        }
        StringBuilder padded = new StringBuilder(secretValue);
        while (padded.length() < MIN_SECRET_LENGTH) {
            padded.append('0');
        }
        return padded.toString();
    }
}
