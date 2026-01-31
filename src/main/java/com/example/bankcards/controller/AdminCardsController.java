package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardAdminUpdateRequest;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.service.card.CardsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Карты (админ)")
@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
public class AdminCardsController {

    private final CardsService cardsService;

    @PostMapping
    @Operation(summary = "Создать карту")
    public CardResponse create(@RequestBody @Valid CardCreateRequest request) {
        return cardsService.createCard(request);
    }

    @PatchMapping("/{id}/block")
    @Operation(summary = "Заблокировать карту")
    public CardResponse block(@PathVariable UUID id) {
        return cardsService.blockCard(id);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Активировать карту")
    public CardResponse activate(@PathVariable UUID id) {
        return cardsService.activateCard(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить карту")
    public void delete(@PathVariable UUID id) {
        cardsService.deleteCard(id);
    }

    @GetMapping
    @Operation(summary = "Поиск карт (фильтры + пагинация)")
    public Page<CardResponse> search(
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) BankCardStatus status,
            @RequestParam(required = false) String last4,
            @ParameterObject Pageable pageable
    ) {
        return cardsService.searchCards(ownerId, status, last4, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить карту по id")
    public CardResponse getById(@PathVariable UUID id) {
        return cardsService.getAdminCardById(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Обновить атрибуты карты (ограниченно)")
    public CardResponse update(
            @PathVariable UUID id,
            @RequestBody @Valid CardAdminUpdateRequest req
    ) {
        return cardsService.updateAdminCard(id, req);
    }
}
