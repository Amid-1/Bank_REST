package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.BalanceResponse;
import com.example.bankcards.dto.card.CardAdminUpdateRequest;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.service.card.mapper.CardMapper;
import com.example.bankcards.service.card.spec.CardsSpecifications;
import com.example.bankcards.util.*;
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
public class CardsServiceImpl implements CardsService {

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

    // USER
    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getMyCards(UUID userId, BankCardStatus status, String last4, Pageable pageable) {
        return searchCards(userId, status, last4, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getMyCardById(UUID userId, UUID cardId) {
        BankCard card = cardsRepository.findByIdAndDeletedFalse(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Карта не найдена: " + cardId));

        if (!card.getOwner().getId().equals(userId)) {
            throw new EntityNotFoundException("Карта не найдена: " + cardId);
        }

        LocalDate today = LocalDate.now();
        return CardMapper.toResponse(card, today);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID userId, UUID cardId) {
        BankCard card = cardsRepository.findByIdAndDeletedFalse(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Карта не найдена: " + cardId));

        if (!card.getOwner().getId().equals(userId)) {
            throw new EntityNotFoundException("Карта не найдена: " + cardId);
        }

        return new BalanceResponse(card.getId(), card.getBalance());
    }

    // ADMIN: READ / SEARCH
    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> searchCards(UUID ownerId, BankCardStatus status, String last4, Pageable pageable) {
        String normalizedLast4 = normalizeLast4OrNull(last4);
        LocalDate today = LocalDate.now();

        Specification<BankCard> spec = Specification
                .where(CardsSpecifications.notDeleted())
                .and(CardsSpecifications.ownerId(ownerId))
                .and(CardsSpecifications.statusWithExpiration(status, today))
                .and(CardsSpecifications.last4(normalizedLast4));

        return cardsRepository.findAll(spec, pageable)
                .map(c -> CardMapper.toResponse(c, today));
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getAdminCardById(UUID cardId) {
        BankCard card = cardsRepository.findByIdAndDeletedFalse(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Карта не найдена: " + cardId));

        LocalDate today = LocalDate.now();
        return CardMapper.toResponse(card, today);
    }

    // ADMIN: COMMANDS
    @Override
    public CardResponse createCard(CardCreateRequest req) {
        AppUser owner = usersRepository.findById(req.ownerId())
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + req.ownerId()));

        String pan = PanNormalizer.normalize(req.cardNumber());

        if (pan.length() < 13 || pan.length() > 19) {
            throw new IllegalArgumentException("Некорректная длина номера карты");
        }

        String masked = CardMasker.mask(pan);

        String panHash = hashEncoder.sha256Hex(pan);

        if (cardsRepository.existsByPanHash(panHash)) {
            throw new IllegalStateException("Карта с таким номером уже существует");
        }

        String encrypted = panEncryptor.encrypt(pan);

        BankCard card = BankCard.builder()
                .owner(owner)
                .expirationDate(req.expirationDate())
                .status(BankCardStatus.ACTIVE)
                .panHash(panHash)
                .encryptedCardNumber(encrypted)
                .maskedCardNumber(masked)
                .deleted(false)
                .build();

        LocalDate today = LocalDate.now();
        return CardMapper.toResponse(cardsRepository.save(card), today);
    }

    @Override
    public CardResponse updateAdminCard(UUID cardId, CardAdminUpdateRequest req) {
        BankCard card = cardsRepository.findByIdAndDeletedFalse(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Карта не найдена: " + cardId));

        if (req.expirationDate() != null) {
            card.setExpirationDate(req.expirationDate());
        }

        LocalDate today = LocalDate.now();
        return CardMapper.toResponse(cardsRepository.save(card), today);
    }

    @Override
    public CardResponse blockCard(UUID cardId) {
        BankCard card = cardsRepository.findByIdAndDeletedFalse(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Карта не найдена: " + cardId));

        card.setStatus(BankCardStatus.BLOCKED);

        LocalDate today = LocalDate.now();
        return CardMapper.toResponse(cardsRepository.save(card), today);
    }

    @Override
    public CardResponse activateCard(UUID cardId) {
        BankCard card = cardsRepository.findByIdAndDeletedFalse(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Карта не найдена: " + cardId));

        LocalDate today = LocalDate.now();
        if (CardStatusUtil.isExpired(card.getExpirationDate(), today)) {
            throw new IllegalStateException("Нельзя активировать просроченную карту");
        }

        card.setStatus(BankCardStatus.ACTIVE);
        return CardMapper.toResponse(cardsRepository.save(card), today);
    }

    // SOFT DELETE
    @Override
    public void deleteCard(UUID cardId) {
        BankCard card = cardsRepository.findByIdAndDeletedFalse(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Карта не найдена: " + cardId));

        card.setDeleted(true);
        card.setStatus(BankCardStatus.BLOCKED);
        cardsRepository.save(card);
    }

    private static String normalizeLast4OrNull(String last4) {
        if (last4 == null) return null;
        String s = last4.trim();
        if (s.isEmpty()) return null;
        if (!s.matches("\\d{4}")) throw new IllegalArgumentException("last4 должен состоять ровно из 4 цифр");
        return s;
    }
}
