package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Запрос на перевод между своими картами")
public record TransferRequest(

        @Schema(description = "ID карты списания", format = "uuid")
        @NotNull(message = "fromCardId обязателен")
        UUID fromCardId,

        @Schema(description = "ID карты зачисления", format = "uuid")
        @NotNull(message = "toCardId обязателен")
        UUID toCardId,

        @Schema(description = "Сумма перевода", example = "100.00", minimum = "0.01")
        @NotNull(message = "amount обязателен")
        @DecimalMin(value = "0.01", message = "amount должен быть >= 0.01")
        @Digits(integer = 12, fraction = 2, message = "amount должен быть в формате до 12 цифр и 2 знаков после запятой")
        BigDecimal amount
) {}
