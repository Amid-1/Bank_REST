package com.example.bankcards.service.auth;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String issueToken(UserDetails user);
    String extractUsername(String token);
    boolean isTokenValid(String token, UserDetails user);
}
