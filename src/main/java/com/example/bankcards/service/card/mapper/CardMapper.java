package com.example.bankcards.service.card.mapper;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;

import java.time.LocalDate;

public final class CardMapper {
    private CardMapper() {}

    private static BankCardStatus effectiveStatus(BankCard card) {
        if (card.getExpirationDate() != null && card.getExpirationDate().isBefore(LocalDate.now())) {
            return BankCardStatus.EXPIRED;
        }
        return card.getStatus();
    }

    public static CardResponse toResponse(BankCard card) {
        return new CardResponse(
                card.getId(),
                card.getMaskedCardNumber(),
                card.getExpirationDate(),
                card.getBalance(),
                effectiveStatus(card),
                card.getOwner().getId()
        );
    }
}
