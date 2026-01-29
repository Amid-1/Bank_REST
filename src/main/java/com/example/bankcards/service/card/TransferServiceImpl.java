package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.dto.card.TransferResponse;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.card.TransferRecord;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.TransferRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final CardsRepository cardsRepository;
    private final TransferRecordsRepository transferRecordsRepository;

    @Override
    @Transactional
    public TransferResponse transfer(UUID userId, TransferRequest request) {
        UUID fromId = request.fromCardId();
        UUID toId = request.toCardId();
        BigDecimal amount = request.amount();

        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("fromCardId must be different from toCardId");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("amount scale must be <= 2");
        }

        // 1) Лочим карты в стабильном порядке, чтобы избежать deadlock
        UUID firstId = (fromId.compareTo(toId) < 0) ? fromId : toId;
        UUID secondId = (firstId.equals(fromId)) ? toId : fromId;

        BankCard first = lockOwnedCard(userId, firstId);
        BankCard second = lockOwnedCard(userId, secondId);

        // 2) from/to после лока
        BankCard from = first.getId().equals(fromId) ? first : second;
        BankCard to = first.getId().equals(toId) ? first : second;

        // 3) Валидации статусов/срока
        ensureTransferable(from);
        ensureTransferable(to);

        BigDecimal fromBalance = safeBalance(from);
        if (fromBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }

        // 4) Балансы (dirty checking) обновление
        from.setBalance(fromBalance.subtract(amount));
        to.setBalance(safeBalance(to).add(amount));

        // 5) Запись о переводе
        TransferRecord record = TransferRecord.builder()
                .fromCard(from)
                .toCard(to)
                .amount(amount)
                .build();

        TransferRecord saved = transferRecordsRepository.saveAndFlush(record);

        return new TransferResponse(
                saved.getId(),
                from.getId(),
                to.getId(),
                amount,
                saved.getCreatedAt(),
                from.getBalance(),
                to.getBalance()
        );
    }

    private BankCard lockOwnedCard(UUID userId, UUID cardId) {
        return cardsRepository.lockByIdAndOwnerId(cardId, userId)
                .orElseThrow(() -> new AccessDeniedException("Card not found or not owned by user: " + cardId));
    }

    private void ensureTransferable(BankCard card) {
        if (card.getExpirationDate() != null && card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Card is expired: " + card.getId());
        }
        if (card.getStatus() != BankCardStatus.ACTIVE) {
            throw new IllegalStateException("Card is not ACTIVE: " + card.getId());
        }
    }

    private static BigDecimal safeBalance(BankCard card) {
        return card.getBalance() == null ? BigDecimal.ZERO : card.getBalance();
    }
}
