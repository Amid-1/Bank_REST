package com.example.bankcards.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Пользователь")
public record UserResponse(
        @Schema(description = "ID пользователя", example = "fc5855a2-58d8-4148-9a54-277f3bcb60d8")
        UUID id,

        @Schema(description = "Имя пользователя", example = "Andy")
        String name,

        @Schema(description = "Email", example = "example@mail.ru")
        String email
) {}
