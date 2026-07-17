package com.genrag.auth.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

@Service
public class JwtService {
    // Constants
    static final String TOKEN_TYPE_IDENTIFIER = "type";
    static final String TYPE_ACCESS = "access";
    static final String TYPE_REFRESH = "refresh";

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(
        @Value("${genrag.jwt.access-secret}") String accessSecret,
        @Value("${genrag.jwt.refresh-secret}") String refreshSecret,
        @Value("${genrag.jwt.access-ttl}") Duration accessTtl,
        @Value("${genrag.jwt.refresh-ttl}") Duration refreshTtl
    ) {
        this.accessKey = hmacEncryptor(accessSecret, "JWT_ACCESS_TOKEN_SECRET");
        this.refreshKey = hmacEncryptor(refreshSecret, "JWT_REFRESH_TOKEN_SECRET");
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    private static SecretKey hmacEncryptor(String secret, String key) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(key + " is not set as an environment variable.");
        }
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (DecodingException | WeakKeyException e) {
            throw new IllegalStateException(key + " is invalid. Error: ", e);
        }
    }

    public String generateAccessToken(UUID userId) {
        return buildToken(userId, TYPE_ACCESS, accessKey, accessTtl, Map.of());
    }

    public String generateRefreshToken(UUID userId) {
        return buildToken(userId, TYPE_REFRESH, refreshKey, refreshTtl, Map.of());
    }

    private String buildToken(UUID userId, String type, SecretKey key, Duration ttl, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        return Jwts
            .builder()
            .claims(extraClaims)
            .subject(userId.toString())
            .claim(TOKEN_TYPE_IDENTIFIER, type)
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(ttl)))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
    }

    public Claims parseAccessToken(String token) {
        return parse(token, accessKey, TYPE_ACCESS);
    }

    public Claims parseRefreshToken(String token) {
        return parse(token, refreshKey, TYPE_REFRESH);
    }

    private Claims parse(String token, SecretKey key, String expectedType) {
        return Jwts.parser()
            .verifyWith(key)
            .require(TOKEN_TYPE_IDENTIFIER, expectedType)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
