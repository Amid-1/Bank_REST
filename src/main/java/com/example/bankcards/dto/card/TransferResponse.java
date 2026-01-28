package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ответ по переводу между своими картами")
public record TransferResponse(
        @Schema(description = "ID перевода")
        UUID id,

        @Schema(description = "ID карты списания")
        UUID fromCardId,

        @Schema(description = "ID карты зачисления")
        UUID toCardId,

        @Schema(description = "Сумма перевода")
        BigDecimal amount,

        @Schema(description = "Дата и время перевода")
        LocalDateTime createdAt,

        @Schema(description = "Баланс карты списания после операции")
        BigDecimal fromBalanceAfter,

        @Schema(description = "Баланс карты зачисления после операции")
        BigDecimal toBalanceAfter
) {}
