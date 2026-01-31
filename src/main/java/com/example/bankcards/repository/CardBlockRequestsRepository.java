package com.example.bankcards.repository;

import com.example.bankcards.entity.request.CardBlockRequest;
import com.example.bankcards.entity.request.CardBlockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CardBlockRequestsRepository extends JpaRepository<CardBlockRequest, UUID>,
        JpaSpecificationExecutor<CardBlockRequest> {

    boolean existsByCard_IdAndStatus(UUID cardId, CardBlockStatus status);

    Page<CardBlockRequest> findAllByStatus(CardBlockStatus status, Pageable pageable);
}
