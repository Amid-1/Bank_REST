package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Запрос на перевод между своими картами")
public record TransferRequest(
        @Schema(description = "ID карты списания")
        @NotNull UUID fromCardId,

        @Schema(description = "ID карты зачисления")
        @NotNull UUID toCardId,

        @Schema(description = "Сумма перевода", example = "100.00")
        @NotNull @Positive BigDecimal amount
) {}
