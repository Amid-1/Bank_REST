package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CardsService {

    // USER
    Page<CardResponse> getMyCards(UUID userId, BankCardStatus status, String last4, Pageable pageable);

    // ADMIN
    CardResponse createCard(CardCreateRequest request);
    CardResponse blockCard(UUID cardId);
    CardResponse activateCard(UUID cardId);
    void deleteCard(UUID cardId);

    Page<CardResponse> searchCards(UUID ownerId, BankCardStatus status, String last4, Pageable pageable);
}
