package com.example.bankcards.service.auth;

import com.example.bankcards.dto.auth.JwtResponse;
import com.example.bankcards.dto.auth.LoginRequest;

public interface AuthService {
    JwtResponse login(LoginRequest request);
}
