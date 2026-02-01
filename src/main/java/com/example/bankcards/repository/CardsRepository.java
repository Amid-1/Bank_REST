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

public interface CardsRepository extends JpaRepository<BankCard, UUID>, JpaSpecificationExecutor<BankCard> {

    boolean existsByPanHash(String panHash);

    Optional<BankCard> findByIdAndDeletedFalse(UUID id);

    Optional<BankCard> findByIdAndOwnerIdAndDeletedFalse(UUID id, UUID ownerId);

    boolean existsByOwnerIdAndDeletedFalse(UUID ownerId);

    boolean existsByOwnerId(UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select c from BankCard c
           where c.id = :id
             and c.owner.id = :ownerId
             and c.deleted = false
           """)
    Optional<BankCard> lockByIdAndOwnerId(@Param("id") UUID id,
                                          @Param("ownerId") UUID ownerId);
}
