package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardBlockRequestResponse;
import com.example.bankcards.entity.request.CardBlockStatus;
import com.example.bankcards.service.card.CardBlockRequestsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Заявки на блокировку (админ)")
@RestController
@RequestMapping("/api/admin/block-requests")
@RequiredArgsConstructor
public class AdminCardBlockRequestsController {

    private final CardBlockRequestsService service;

    @GetMapping
    @Operation(summary = "Список заявок по статусу")
    public Page<CardBlockRequestResponse> getAll(
            @RequestParam(defaultValue = "WAITING") CardBlockStatus status,
            Pageable pageable
    ) {
        return service.getAll(status, pageable);
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Одобрить заявку (карта - BLOCKED, заявка - APPROVED)")
    public void approve(@PathVariable UUID id) {
        service.approve(id);
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Отклонить заявку (заявка - REJECTED)")
    public void reject(@PathVariable UUID id) {
        service.reject(id);
    }
}