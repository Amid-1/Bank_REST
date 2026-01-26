package com.example.bankcards.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с JWT токеном доступа")
public record JwtResponse(
        @Schema(description = "JWT access token")
        String token
) {}
