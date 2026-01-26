package com.example.bankcards.service.card.mapper;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCard;

public final class CardMapper {
    private CardMapper() {}

    public static CardResponse toResponse(BankCard card) {
        return new CardResponse(
                card.getId(),
                card.getMaskedCardNumber(),
                card.getExpirationDate(),
                card.getBalance(),
                card.getStatus(),
                card.getOwner().getId()
        );
    }
}