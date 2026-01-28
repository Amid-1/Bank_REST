package com.example.bankcards.service.card.spec;

import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class CardsSpecifications {

    private CardsSpecifications() {}

    public static Specification<BankCard> ownerId(UUID ownerId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<BankCard> status(BankCardStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<BankCard> last4(String last4) {
        return (root, query, cb) -> cb.like(root.get("maskedCardNumber"), "%" + last4);
    }
}
