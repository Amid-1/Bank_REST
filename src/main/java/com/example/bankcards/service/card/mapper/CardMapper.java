package com.example.bankcards.service.card.mapper;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.util.CardStatusUtil;

import java.time.LocalDate;

public final class CardMapper {

    private CardMapper() {}

    public static CardResponse toResponse(BankCard c, LocalDate today) {
        BankCardStatus effective = CardStatusUtil.effectiveStatus(c, today);

        return new CardResponse(
                c.getId(),
                c.getMaskedCardNumber(),
                c.getExpirationDate(),
                c.getBalance(),
                effective,
                c.getOwner().getId()
        );
    }
}
