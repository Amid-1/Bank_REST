package com.example.bankcards.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Сброс пароля пользователя администратором")
public record AdminPasswordResetRequest(

        @Schema(description = "Новый пароль", example = "new_strong_password_123", minLength = 10, maxLength = 255)
        @NotBlank(message = "newPassword обязателен")
        @Size(min = 4, max = 255, message = "newPassword должен быть длиной 4-255 символов")
        String newPassword
) {}
