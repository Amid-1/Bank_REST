package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardBlockRequestCreate;
import com.example.bankcards.dto.card.CardBlockRequestResponse;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.service.card.CardBlockRequestsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Заявки на блокировку (пользователь)")
@RestController
@RequestMapping("/api/block-requests")
@RequiredArgsConstructor
public class CardBlockRequestsController {

    private final CardBlockRequestsService service;

    @PostMapping
    @Operation(summary = "Создать заявку на блокировку карты")
    public CardBlockRequestResponse create(
            @AuthenticationPrincipal AppUser user,
            @RequestBody @Valid CardBlockRequestCreate request
    ) {
        return service.create(user.getId(), request);
    }
}