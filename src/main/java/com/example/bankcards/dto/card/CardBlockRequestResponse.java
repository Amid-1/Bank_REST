package com.example.bankcards.dto.card;

import com.example.bankcards.entity.request.CardBlockStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ответ по заявке на блокировку")
public record CardBlockRequestResponse(
        UUID id,
        UUID cardId,
        UUID initiatorId,
        CardBlockStatus status,
        String reason,
        LocalDateTime createdAt
) {}
