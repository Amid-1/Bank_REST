package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Запрос пользователя на блокировку карты")
public record CardBlockRequestCreate(

        @Schema(description = "ID карты", example = "fc5855a2-58d8-4148-9a54-277f3bcb60d8", format = "uuid")
        @NotNull(message = "cardId обязателен")
        UUID cardId,

        @Schema(description = "Причина (необязательно)", example = "Утеряна карта", maxLength = 500)
        @Size(max = 500, message = "reason должен быть не длиннее 500 символов")
        String reason
) {}
