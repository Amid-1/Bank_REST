package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.BalanceResponse;
import com.example.bankcards.dto.card.CardAdminUpdateRequest;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CardsService {

    // USER
    Page<CardResponse> getMyCards(UUID userId, BankCardStatus status, String last4, Pageable pageable);
    CardResponse getMyCardById(UUID userId, UUID cardId);
    BalanceResponse getBalance(UUID userId, UUID cardId);

    // ADMIN (Read/Search)
    Page<CardResponse> searchCards(UUID ownerId, BankCardStatus status, String last4, Pageable pageable);
    CardResponse getAdminCardById(UUID cardId);

    // ADMIN (Commands)
    CardResponse createCard(CardCreateRequest request);
    CardResponse updateAdminCard(UUID cardId, CardAdminUpdateRequest req);
    CardResponse blockCard(UUID cardId);
    CardResponse activateCard(UUID cardId);
    void deleteCard(UUID cardId);
}

