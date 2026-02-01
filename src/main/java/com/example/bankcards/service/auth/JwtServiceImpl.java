package com.example.bankcards.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private static final int MIN_HS256_KEY_BYTES = 32;

    private final SecretKey key;
    private final long expirationMs;

    public JwtServiceImpl(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long expirationMs
    ) {
        byte[] keyBytes = parseSecret(jwtSecret);

        if (keyBytes.length < MIN_HS256_KEY_BYTES) {
            throw new IllegalStateException(
                    "Секрет JWT слишком короткий: для HS256 нужно минимум " + MIN_HS256_KEY_BYTES
                            + " байт, получено " + keyBytes.length + " байт"
            );
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    static byte[] parseSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Секрет JWT не задан (jwt.secret пустой)");
        }

        String v = secret.trim();

        if (v.startsWith("base64:")) {
            String payload = v.substring("base64:".length()).trim();
            if (payload.isEmpty()) {
                throw new IllegalStateException("Секрет JWT в формате base64 задан пустым");
            }

            try {
                return Decoders.BASE64.decode(payload);
            } catch (RuntimeException e) {
                throw new IllegalStateException("Секрет JWT в формате base64 задан некорректно", e);
            }
        }

        if (v.startsWith("raw:")) {
            String payload = v.substring("raw:".length()).trim();
            if (payload.isEmpty()) {
                throw new IllegalStateException("Секрет JWT в формате raw задан пустым");
            }
            return payload.getBytes(StandardCharsets.UTF_8);
        }

        return v.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String issueToken(UserDetails user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public boolean isTokenValid(String token, UserDetails user) {
        Claims claims = parseClaims(token);
        return claims.getSubject().equals(user.getUsername())
                && claims.getExpiration().after(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
