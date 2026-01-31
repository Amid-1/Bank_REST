package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardBlockRequestResponse;
import com.example.bankcards.entity.request.CardBlockStatus;
import com.example.bankcards.service.card.CardBlockRequestsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
            @RequestParam(required = false, defaultValue = "WAITING") CardBlockStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort
    ) {
        Sort s = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            s = Sort.by(sort.stream().map(Sort.Order::by).toList());
        }

        Pageable pageable = PageRequest.of(page, size, s);
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
