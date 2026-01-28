package com.example.bankcards.controller;

import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.dto.card.TransferResponse;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.service.card.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Переводы (пользователь)")
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Перевод между своими картами")
    public TransferResponse transfer(
            @AuthenticationPrincipal AppUser user,
            @RequestBody @Valid TransferRequest request
    ) {
        return transferService.transfer(user.getId(), request);
    }
}
