package com.example.bankcards.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на включение/отключение пользователя")
public record UserEnabledUpdateRequest(
        @NotNull
        @Schema(description = "Флаг активности аккаунта", example = "false")
        Boolean enabled
) {}
