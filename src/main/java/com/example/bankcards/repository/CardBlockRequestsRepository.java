package com.example.bankcards.repository;

import com.example.bankcards.entity.request.CardBlockRequest;
import com.example.bankcards.entity.request.CardBlockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Репозиторий заявок на блокировку карты.
 */
public interface CardBlockRequestsRepository extends JpaRepository<CardBlockRequest, UUID>,
        JpaSpecificationExecutor<CardBlockRequest> {

    // Запретить плодить несколько WAITING заявок по одной карте
    boolean existsByCard_IdAndStatus(UUID cardId, CardBlockStatus status);

    // Админский список по статусу
    Page<CardBlockRequest> findAllByStatus(CardBlockStatus status, Pageable pageable);
}
