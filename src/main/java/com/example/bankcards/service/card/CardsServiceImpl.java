package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.util.PanEncryptor;
import com.example.bankcards.util.PepperHashEncoder;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class CardsServiceImpl {

    private final CardsRepository cardsRepository;
    private final UsersRepository usersRepository;
    private final PepperHashEncoder hashEncoder;
    private final PanEncryptor panEncryptor;

    public CardsServiceImpl(
            CardsRepository cardsRepository,
            UsersRepository usersRepository,
            PepperHashEncoder hashEncoder,
            PanEncryptor panEncryptor
    ) {
        this.cardsRepository = cardsRepository;
        this.usersRepository = usersRepository;
        this.hashEncoder = hashEncoder;
        this.panEncryptor = panEncryptor;
    }

    public CardResponse createCard(CardCreateRequest req) {
        AppUser owner = usersRepository.findById(req.ownerId())
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + req.ownerId()));

        String pan = normalizePan(req.cardNumber());
        String last4 = pan.substring(pan.length() - 4);
        String masked = "**** **** **** " + last4;

        String panHash = hashEncoder.sha256Hex(pan);
        if (cardsRepository.existsByPanHash(panHash)) {
            throw new IllegalStateException("дубликат panHash: " + panHash);
        }

        String encrypted = panEncryptor.encrypt(pan);

        BankCard card = BankCard.builder()
                .owner(owner)
                .expirationDate(req.expirationDate())
                .status(BankCardStatus.ACTIVE)
                .panHash(panHash)
                .encryptedCardNumber(encrypted)
                .maskedCardNumber(masked)
                .build();

        BankCard saved = cardsRepository.save(card);
        return toResponse(saved);
    }

    public CardResponse activateCard(UUID cardId) {
        BankCard card = cardsRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Карта не найдена: " + cardId));

        if (card.getExpirationDate() != null && card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Нельзя активировать просроченную карту");
        }

        card.setStatus(BankCardStatus.ACTIVE);
        return toResponse(cardsRepository.save(card));
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getMyCards(UUID ownerId, BankCardStatus status, String last4, Pageable pageable) {
        String normalizedLast4 = normalizeLast4(last4);

        Specification<BankCard> spec = (root, query, cb) ->
                cb.equal(root.get("owner").get("id"), ownerId);

        if (status != null) {
            if (status == BankCardStatus.EXPIRED) {
                spec = spec.and((root, query, cb) ->
                        cb.lessThan(root.get("expirationDate"), LocalDate.now()));
            } else {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
            }
        }

        if (normalizedLast4 != null) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("maskedCardNumber"), "%" + normalizedLast4));
        }

        return cardsRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private static String normalizePan(String pan) {
        if (pan == null) {
            throw new IllegalArgumentException("cardNumber не должен быть null");
        }
        String digits = pan.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            throw new IllegalArgumentException("cardNumber слишком короткий");
        }
        return digits;
    }

    private static String normalizeLast4(String last4) {
        if (last4 == null) return null;
        String s = last4.trim();
        if (!s.matches("\\d{4}")) {
            throw new IllegalArgumentException("last4 должен состоять ровно из 4 цифр");
        }
        return s;
    }

    private CardResponse toResponse(BankCard c) {
        return new CardResponse(
                c.getId(),
                c.getMaskedCardNumber(),
                c.getExpirationDate(),
                c.getBalance(),
                c.getStatus(),
                c.getOwner().getId()
        );
    }
}
