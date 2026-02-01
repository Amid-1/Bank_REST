package com.example.bankcards.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на аутентификацию")
public record LoginRequest(

        @Schema(description = "Email", example = "example@mail.ru", format = "email", maxLength = 254)
        @NotBlank(message = "email обязателен")
        @Email(message = "email должен быть валидным email-адресом")
        @Size(max = 254, message = "email должен быть не длиннее 254 символов")
        String email,

        @Schema(description = "Пароль", example = "qwerty_best_password", maxLength = 255)
        @NotBlank(message = "password обязателен")
        @Size(max = 255, message = "password должен быть не длиннее 255 символов")
        String password
) {}
