package com.example.bankcards.entity.card;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transfers_record")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "from_card_id")
    private BankCard fromCard;

    @ManyToOne
    @JoinColumn(name = "to_card_id")
    private BankCard toCard;

    private BigDecimal amount;
}
