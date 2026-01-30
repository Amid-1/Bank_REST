package com.example.bankcards.util;

import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;

import java.time.LocalDate;

public final class CardStatusUtil {

    private CardStatusUtil() {}

    public static boolean isExpired(LocalDate exp, LocalDate today) {
        return exp != null && exp.isBefore(today);
    }

    public static BankCardStatus effectiveStatus(BankCard card, LocalDate today) {
        return isExpired(card.getExpirationDate(), today)
                ? BankCardStatus.EXPIRED
                : card.getStatus();
    }
}
