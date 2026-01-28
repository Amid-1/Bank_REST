package com.example.bankcards.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Сброс пароля пользователя администратором")
public record AdminPasswordResetRequest(
        @NotBlank
        @Size(min = 10, max = 255)
        @Schema(description = "Новый пароль", example = "new_strong_password_123")
        String newPassword
) {}
