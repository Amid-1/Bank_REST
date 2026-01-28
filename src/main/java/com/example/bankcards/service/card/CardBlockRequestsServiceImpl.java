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

        BankCard card = cardsRepository.findByIdAndOwner_Id(dto.cardId(), userId)
                .orElseThrow(() -> new AccessDeniedException("Card not found or not owned by user"));

        // карта должна быть активна
        if (card.getStatus() != BankCardStatus.ACTIVE) {
            throw new IllegalStateException("Card is not ACTIVE");
        }

        // карта не должна быть просрочена
        if (card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Card is expired");
        }

        // нельзя создать 2 WAITING-заявки на одну карту
        if (blockRequestsRepository.existsByCard_IdAndStatus(card.getId(), CardBlockStatus.WAITING)) {
            throw new IllegalStateException("Waiting request for this card already exists");
        }

        AppUser initiator = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

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
                .orElseThrow(() -> new EntityNotFoundException("Request not found: " + requestId));

        if (req.getStatus() != CardBlockStatus.WAITING) {
            throw new IllegalStateException("Request is not WAITING");
        }

        BankCard card = cardsRepository.findById(req.getCard().getId())
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + req.getCard().getId()));

        // Блокировка карты и статус заявки APPROVED
        card.setStatus(BankCardStatus.BLOCKED);
        req.setStatus(CardBlockStatus.APPROVED);
    }

    @Override
    @Transactional
    public void reject(UUID requestId) {
        CardBlockRequest req = blockRequestsRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found: " + requestId));

        if (req.getStatus() != CardBlockStatus.WAITING) {
            throw new IllegalStateException("Request is not WAITING");
        }

        req.setStatus(CardBlockStatus.REJECTED);
    }
}