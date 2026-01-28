package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.service.card.CardsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Карты (пользователь)")
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardsController {

    private final CardsService cardsService;

    @GetMapping
    @Operation(summary = "Список моих карт (фильтры + пагинация)")
    public Page<CardResponse> getMyCards(
            @AuthenticationPrincipal AppUser user,
            @RequestParam(required = false) BankCardStatus status,
            @RequestParam(required = false) String last4,
            Pageable pageable
    ) {
        return cardsService.getMyCards(user.getId(), status, last4, pageable);
    }
}
