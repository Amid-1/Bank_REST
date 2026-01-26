package com.example.bankcards.dto.card;

import com.example.bankcards.entity.card.BankCardStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ответ с информацией о карте")
public record CardResponse(
        @Schema(description = "ID карты")
        UUID id,

        @Schema(description = "Замаскированный номер", example = "**** **** **** 1111")
        String maskedCardNumber,

        @Schema(description = "Дата окончания действия", example = "2035-08-11")
        LocalDate expirationDate,

        @Schema(description = "Баланс", example = "100.00")
        BigDecimal balance,

        @Schema(description = "Статус карты")
        BankCardStatus status,

        @Schema(description = "ID владельца")
        UUID ownerId
) {}
