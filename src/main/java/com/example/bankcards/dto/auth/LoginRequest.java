package com.example.bankcards.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на аутентификацию")
public record LoginRequest(
        @Schema(description = "Email", example = "example@mail.ru")
        @NotBlank @Email
        String email,

        @Schema(description = "Пароль", example = "qwerty_best_password")
        @NotBlank
        String password
) {}
