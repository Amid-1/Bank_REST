package com.example.bankcards.service.block.mapper;

import com.example.bankcards.dto.card.CardBlockRequestResponse;
import com.example.bankcards.entity.request.CardBlockRequest;

public final class CardBlockRequestMapper {
    private CardBlockRequestMapper() {}

    public static CardBlockRequestResponse toResponse(CardBlockRequest r) {
        return new CardBlockRequestResponse(
                r.getId(),
                r.getCard().getId(),
                r.getInitiator().getId(),
                r.getStatus(),
                r.getReason(),
                r.getCreatedAt()
        );
    }
}