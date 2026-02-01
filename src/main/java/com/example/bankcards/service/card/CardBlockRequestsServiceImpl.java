package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.CardBlockRequestCreate;
import com.example.bankcards.dto.card.CardBlockRequestResponse;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.request.CardBlockRequest;
import com.example.bankcards.entity.request.CardBlockStatus;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.repository.CardBlockRequestsRepository;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.service.card.mapper.CardBlockRequestMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardBlockRequestsServiceImpl implements CardBlockRequestsService {

    private final CardBlockRequestsRepository blockRequestsRepository;
    private final CardsRepository cardsRepository;
    private final UsersRepository usersRepository;

    @Override
    @Transactional
    public CardBlockRequestResponse create(UUID userId, CardBlockRequestCreate dto) {

        BankCard card = cardsRepository.findByIdAndOwnerIdAndDeletedFalse(dto.cardId(), userId)
                .orElseThrow(() -> new AccessDeniedException("Карта не найдена или не принадлежит пользователю"));

        if (card.getStatus() != BankCardStatus.ACTIVE) {
            throw new IllegalStateException("Карта не находится в статусе ACTIVE");
        }

        if (card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Срок действия карты истек");
        }

        if (blockRequestsRepository.existsByCard_IdAndStatus(card.getId(), CardBlockStatus.WAITING)) {
            throw new IllegalStateException("Заявка на блокировку по данной карте уже существует");
        }

        AppUser initiator = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        CardBlockRequest req = CardBlockRequest.builder()
                .card(card)
                .initiator(initiator)
                .status(CardBlockStatus.WAITING)
                .reason(dto.reason())
                .build();

        CardBlockRequest saved = blockRequestsRepository.save(req);
        return CardBlockRequestMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardBlockRequestResponse> getAll(CardBlockStatus status, Pageable pageable) {
        return blockRequestsRepository.findAllByStatus(status, pageable)
                .map(CardBlockRequestMapper::toResponse);
    }

    @Override
    @Transactional
    public void approve(UUID requestId) {
        CardBlockRequest req = blockRequestsRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена: " + requestId));

        if (req.getStatus() != CardBlockStatus.WAITING) {
            throw new IllegalStateException("Заявка не находится в статусе WAITING");
        }

        BankCard card = cardsRepository.findByIdAndDeletedFalse(req.getCard().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Карта не найдена или удалена: " + req.getCard().getId()
                ));

        card.setStatus(BankCardStatus.BLOCKED);
        req.setStatus(CardBlockStatus.APPROVED);
    }

    @Override
    @Transactional
    public void reject(UUID requestId) {
        CardBlockRequest req = blockRequestsRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена: " + requestId));

        if (req.getStatus() != CardBlockStatus.WAITING) {
            throw new IllegalStateException("Заявка не находится в статусе WAITING");
        }

        req.setStatus(CardBlockStatus.REJECTED);
    }
}
