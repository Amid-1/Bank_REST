package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.CardBlockRequestCreate;
import com.example.bankcards.dto.card.CardBlockRequestResponse;
import com.example.bankcards.entity.request.CardBlockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CardBlockRequestsService {

    CardBlockRequestResponse create(UUID userId, CardBlockRequestCreate request);

    Page<CardBlockRequestResponse> getAll(CardBlockStatus status, Pageable pageable);

    void approve(UUID requestId);
    void reject(UUID requestId);
}