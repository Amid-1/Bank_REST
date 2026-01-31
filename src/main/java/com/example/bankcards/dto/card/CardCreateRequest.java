package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Запрос на создание карты (обычно админский)")
public record CardCreateRequest(

        @Schema(description = "Номер карты (допустимы цифры, пробелы и дефисы)", example = "4111 1111 1111 1111",
                minLength = 16, maxLength = 23, pattern = "^[0-9\\s-]+$")
        @NotBlank(message = "cardNumber обязателен")
        @Size(min = 16, max = 23, message = "cardNumber должен быть длиной 16-23 символа (с пробелами/дефисами)")
        @Pattern(regexp = "^[0-9\\s-]+$", message = "cardNumber может содержать только цифры, пробелы и дефисы")
        String cardNumber,

        @Schema(description = "Дата окончания действия карты", example = "2035-08-11")
        @NotNull(message = "expirationDate обязателен")
        @FutureOrPresent(message = "expirationDate должна быть в будущем или сегодня")
        LocalDate expirationDate,

        @Schema(description = "ID владельца карты", example = "5545749c-5bb2-40a5-a049-52ecf74b2571")
        @NotNull(message = "ownerId обязателен")
        UUID ownerId
) {}
