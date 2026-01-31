package com.example.bankcards.service.card.spec;

import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class CardsSpecifications {

    private CardsSpecifications() {}

    public static Specification<BankCard> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<BankCard> ownerId(UUID ownerId) {
        if (ownerId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<BankCard> statusWithExpiration(BankCardStatus status, LocalDate today) {
        if (status == null) return null;
        LocalDate d = (today != null) ? today : LocalDate.now();

        if (status == BankCardStatus.EXPIRED) {
            return (root, query, cb) -> cb.and(
                    cb.isNotNull(root.get("expirationDate")),
                    cb.lessThan(root.get("expirationDate"), d)
            );
        }

        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), status),
                cb.or(
                        cb.isNull(root.get("expirationDate")),
                        cb.greaterThanOrEqualTo(root.get("expirationDate"), d)
                )
        );
    }

    public static Specification<BankCard> last4(String last4) {
        if (last4 == null) return null;
        String s = last4.trim();
        if (s.isEmpty()) return null;

        return (root, query, cb) -> cb.like(root.get("maskedCardNumber"), "%" + s);
    }
}
