package com.example.bankcards.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на создание пользователя")
public record UserCreateRequest(

        @Schema(description = "Имя пользователя", example = "Andrey", minLength = 1, maxLength = 50)
        @NotBlank(message = "name обязателен")
        @Size(max = 50, message = "name должен быть не длиннее 50 символов")
        String name,

        @Schema(description = "Email", example = "example@mail.ru", format = "email", maxLength = 254)
        @NotBlank(message = "email обязателен")
        @Email(message = "email должен быть валидным email-адресом")
        @Size(max = 254, message = "email должен быть не длиннее 254 символов")
        String email,

        @Schema(description = "Пароль", example = "qwerty_best_password", minLength = 10, maxLength = 255)
        @NotBlank(message = "password обязателен")
        @Size(min = 4, max = 255, message = "password должен быть длиной 4-255 символов")
        String password
) {}
