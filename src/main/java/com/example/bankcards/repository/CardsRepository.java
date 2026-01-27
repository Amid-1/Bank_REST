package com.example.bankcards.repository;

import com.example.bankcards.entity.card.BankCard;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий банковских карт.
 * Поддерживает:
 *  - поиск с фильтрами и пагинацией (через Specification)
 *  - блокировку строк (PESSIMISTIC_WRITE) для переводов
 */
public interface CardsRepository extends JpaRepository<BankCard, UUID>, JpaSpecificationExecutor<BankCard> {

    boolean existsByPanHash(String panHash);

    // обычная проверка владения (без блокировки)
    Optional<BankCard> findByIdAndOwner_Id(UUID id, UUID ownerId);

    // для перевода (с блокировкой)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BankCard c where c.id = :id and c.owner.id = :ownerId")
    Optional<BankCard> lockByIdAndOwnerId(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
