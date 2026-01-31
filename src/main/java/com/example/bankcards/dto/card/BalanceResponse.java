package com.example.bankcards.dto.card;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(
        UUID cardId,
        BigDecimal balance
) {}