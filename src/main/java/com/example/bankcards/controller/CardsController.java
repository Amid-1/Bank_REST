package com.example.bankcards.controller;

import com.example.bankcards.dto.card.BalanceResponse;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.service.card.CardsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
            @ParameterObject Pageable pageable
    ) {
        return cardsService.getMyCards(user.getId(), status, last4, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить мою карту по id")
    public CardResponse getMyCard(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id
    ) {
        return cardsService.getMyCardById(user.getId(), id);
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Получить баланс моей карты")
    public BalanceResponse getBalance(@AuthenticationPrincipal AppUser user,
                                      @PathVariable UUID id) {
        return cardsService.getBalance(user.getId(), id);
    }

}
