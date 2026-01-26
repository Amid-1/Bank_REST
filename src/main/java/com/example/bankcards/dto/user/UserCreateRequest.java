package com.example.bankcards.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на создание пользователя")
public record UserCreateRequest(
        @Schema(description = "Имя пользователя", example = "Andy")
        @NotBlank @Size(max = 50)
        String name,

        @Schema(description = "Email", example = "example@mail.ru")
        @NotBlank @Email
        String email,

        @Schema(description = "Пароль", example = "qwerty_best_password")
        @NotBlank @Size(min = 10, max = 255)
        String password
) {}