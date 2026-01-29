package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.service.card.mapper.CardMapper;
import com.example.bankcards.service.card.spec.CardsSpecifications;
import com.example.bankcards.util.CardMasker;
import com.example.bankcards.util.PanEncryptor;
import com.example.bankcards.util.PanNormalizer;
import com.example.bankcards.util.PepperHashEncoder;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardsServiceImpl implements CardsService {

    private final CardsRepository cardsRepository;
    private final UsersRepository usersRepository;
    private final PepperHashEncoder hashEncoder;
    private final PanEncryptor panEncryptor;

    // USER
    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getMyCards(UUID userId, BankCardStatus status, String last4, Pageable pageable) {
        Specification<BankCard> spec = CardsSpecifications.ownerId(userId);

        if (status != null) {
            spec = spec.and(CardsSpecifications.status(status));
        }

        String l4 = normalizeLast4(last4);
        if (l4 != null) {
            spec = spec.and(CardsSpecifications.last4(l4));
        }

        return cardsRepository.findAll(spec, pageable).map(CardMapper::toResponse);
    }

    // ADMIN
    @Override
    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        AppUser owner = usersRepository.findById(request.ownerId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.ownerId()));

        String pan = PanNormalizer.normalize(request.cardNumber());

        // pan_hash = sha256(pan + pepper) — для дублей
        String panHash = hashEncoder.sha256Hex(pan);
        if (cardsRepository.existsByPanHash(panHash)) {
            throw new IllegalStateException("Card already exists (panHash duplicate)");
        }

        String masked = CardMasker.mask(pan);
        String encrypted = panEncryptor.encrypt(pan);

        BankCardStatus status = request.expirationDate().isBefore(LocalDate.now())
                ? BankCardStatus.EXPIRED
                : BankCardStatus.ACTIVE;

        BankCard card = BankCard.builder()
                .owner(owner)
                .panHash(panHash)
                .maskedCardNumber(masked)
                .encryptedCardNumber(encrypted)
                .expirationDate(request.expirationDate())
                .status(status)
                .build();

        BankCard saved = cardsRepository.save(card);
        return CardMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CardResponse blockCard(UUID cardId) {
        BankCard card = cardsRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + cardId));

        card.setStatus(BankCardStatus.BLOCKED);
        return CardMapper.toResponse(card);
    }

    @Override
    @Transactional
    public CardResponse activateCard(UUID cardId) {
        BankCard card = cardsRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + cardId));

        if (card.getExpirationDate() != null && card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Cannot activate expired card: " + cardId);
        }

        card.setStatus(BankCardStatus.ACTIVE);
        return CardMapper.toResponse(card);
    }

    @Override
    @Transactional
    public void deleteCard(UUID cardId) {
        if (!cardsRepository.existsById(cardId)) {
            throw new EntityNotFoundException("Card not found: " + cardId);
        }
        cardsRepository.deleteById(cardId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> searchCards(UUID ownerId, BankCardStatus status, String last4, Pageable pageable) {
        Specification<BankCard> spec = (root, query, cb) -> cb.conjunction();

        if (ownerId != null) {
            spec = spec.and(CardsSpecifications.ownerId(ownerId));
        }
        if (status != null) {
            spec = spec.and(CardsSpecifications.status(status));
        }

        String l4 = normalizeLast4(last4);
        if (l4 != null) {
            spec = spec.and(CardsSpecifications.last4(l4));
        }

        return cardsRepository.findAll(spec, pageable).map(CardMapper::toResponse);
    }

    private static String normalizeLast4(String last4) {
        if (last4 == null || last4.isBlank()) return null;

        String l4 = last4.trim();
        if (!l4.matches("\\d{4}")) {
            throw new IllegalArgumentException("last4 must be exactly 4 digits");
        }
        return l4;
    }
}
