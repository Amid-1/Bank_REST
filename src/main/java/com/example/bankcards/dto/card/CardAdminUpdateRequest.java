package com.example.bankcards.dto.card;

import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDate;

public record CardAdminUpdateRequest(
        @FutureOrPresent(message = "expirationDate должна быть в будущем или сегодня")
        LocalDate expirationDate
) {}

