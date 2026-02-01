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

        @Schema(
                description = "Номер карты: 16 цифр. Допустимы пробелы или дефисы между группами по 4 цифры.",
                example = "4111 1111 1111 1111",
                minLength = 16,
                maxLength = 19,
                pattern = "^\\d{4}(?:[ -]?\\d{4}){3}$"
        )
        @NotBlank(message = "cardNumber обязателен")
        @Size(min = 16, max = 19, message = "cardNumber должен быть длиной 16-19 символов")
        @Pattern(
                regexp = "^\\d{4}(?:[ -]?\\d{4}){3}$",
                message = "cardNumber должен содержать 16 цифр; пробелы/дефисы допустимы между четверками"
        )
        String cardNumber,

        @Schema(description = "Дата окончания действия карты", example = "2035-08-11", format = "date")
        @NotNull(message = "expirationDate обязателен")
        @FutureOrPresent(message = "expirationDate должна быть в будущем или сегодня")
        LocalDate expirationDate,

        @Schema(description = "ID владельца карты", example = "5545749c-5bb2-40a5-a049-52ecf74b2571", format = "uuid")
        @NotNull(message = "ownerId обязателен")
        UUID ownerId
) {}
