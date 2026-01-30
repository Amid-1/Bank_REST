package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.dto.card.TransferResponse;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.card.TransferRecord;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.TransferRecordsRepository;
import jakarta.persistence.EntityNotFoundException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class TransferServiceImpl implements TransferService {

    private final CardsRepository cardsRepository;
    private final TransferRecordsRepository transferRecordsRepository;

    public TransferServiceImpl(CardsRepository cardsRepository,
                               TransferRecordsRepository transferRecordsRepository) {
        this.cardsRepository = cardsRepository;
        this.transferRecordsRepository = transferRecordsRepository;
    }

    @Override
    public TransferResponse transfer(UUID userId, TransferRequest req) {
        if (userId == null) throw new IllegalArgumentException("userId is null");
        if (req == null) throw new IllegalArgumentException("request is null");

        UUID fromId = req.fromCardId();
        UUID toId = req.toCardId();
        BigDecimal amount = getBigDecimal(req, fromId, toId);

        UUID first = (fromId.compareTo(toId) < 0) ? fromId : toId;
        UUID second = first.equals(fromId) ? toId : fromId;

        BankCard c1 = cardsRepository.lockByIdAndOwnerId(first, userId)
                .orElseThrow(() -> new EntityNotFoundException("Карта не найдена: " + first));
        BankCard c2 = cardsRepository.lockByIdAndOwnerId(second, userId)
                .orElseThrow(() -> new EntityNotFoundException("Карта не найдена: " + second));

        BankCard from = c1.getId().equals(fromId) ? c1 : c2;
        BankCard to = (from == c1) ? c2 : c1;

        ensureTransferable(from);
        ensureTransferable(to);

        if (from.getBalance() == null) from.setBalance(BigDecimal.ZERO);
        if (to.getBalance() == null) to.setBalance(BigDecimal.ZERO);

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Недостаточно средств");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        TransferRecord record = TransferRecord.builder()
                .fromCard(from)
                .toCard(to)
                .amount(amount)
                .build();

        TransferRecord saved = transferRecordsRepository.save(record);

        return toResponse(saved, from.getBalance(), to.getBalance());
    }

    private static @NonNull BigDecimal getBigDecimal(TransferRequest req, UUID fromId, UUID toId) {
        BigDecimal amount = req.amount();

        if (fromId == null || toId == null) throw new IllegalArgumentException("cardId is null");

        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("fromCardId и toCardId должны быть разными");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount должен быть > 0");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("amount: не более 2 знаков после запятой");
        }
        return amount;
    }

    private void ensureTransferable(BankCard card) {
        if (card.getStatus() != BankCardStatus.ACTIVE) {
            throw new IllegalStateException("Карта не в статусе ACTIVE: " + card.getId());
        }
        LocalDate exp = card.getExpirationDate();
        if (exp != null && exp.isBefore(LocalDate.now())) {
            throw new IllegalStateException("Карта просрочена: " + card.getId());
        }
    }

    private TransferResponse toResponse(TransferRecord record,
                                        BigDecimal fromBalanceAfter,
                                        BigDecimal toBalanceAfter) {
        return new TransferResponse(
                record.getId(),
                record.getFromCard().getId(),
                record.getToCard().getId(),
                record.getAmount(),
                record.getCreatedAt(),
                fromBalanceAfter,
                toBalanceAfter
        );
    }
}
