package com.example.bankcards.repository;

import com.example.bankcards.entity.card.TransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Репозиторий записей о переводах.
 */
public interface TransferRecordsRepository extends JpaRepository<TransferRecord, UUID> {
}
