package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.CreditCardNumber;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Запрос на создание карты (обычно админский)")
public record CardCreateRequest(
        @Schema(description = "Номер карты", example = "4111111111111111")
        @NotBlank
        @CreditCardNumber
        String cardNumber,

        @Schema(description = "Дата окончания действия карты", example = "2035-08-11")
        @NotNull
        @FutureOrPresent
        LocalDate expirationDate,

        @Schema(description = "ID владельца карты", example = "5545749c-5bb2-40a5-a049-52ecf74b2571")
        @NotNull
        UUID ownerId
) {}
