package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.dto.card.TransferResponse;

import java.util.UUID;

public interface TransferService {
    TransferResponse transfer(UUID userId, TransferRequest request);
}
