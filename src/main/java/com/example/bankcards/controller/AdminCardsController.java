package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardAdminUpdateRequest;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.service.card.CardsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.example.bankcards.util.SortParser.parseSort;

@Tag(name = "Карты (админ)")
@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
public class AdminCardsController {

    private final CardsService cardsService;

    @PostMapping
    @Operation(summary = "Создать карту")
    public ResponseEntity<CardResponse> create(@RequestBody @Valid CardCreateRequest request) {
        CardResponse created = cardsService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Поиск карт (фильтры + пагинация)")
    public Page<CardResponse> search(
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) BankCardStatus status,
            @RequestParam(required = false) @Size(min = 4, max = 4) String last4,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort
    ) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);
        return cardsService.searchCards(ownerId, status, last4, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить карту по id")
    public CardResponse getById(@PathVariable UUID id) {
        return cardsService.getAdminCardById(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Обновить атрибуты карты (ограниченно)")
    public CardResponse update(@PathVariable UUID id,
                               @RequestBody @Valid CardAdminUpdateRequest req) {
        return cardsService.updateAdminCard(id, req);
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
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        cardsService.deleteCard(id);
        return ResponseEntity.noContent().build(); // 204
    }
}
