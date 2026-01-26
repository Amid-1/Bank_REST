package com.example.bankcards.entity.card;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
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

    @ManyToOne(optional = false)
    @JoinColumn(name = "from_card_id", nullable = false)
    private BankCard fromCard;

    @ManyToOne(optional = false)
    @JoinColumn(name = "to_card_id", nullable = false)
    private BankCard toCard;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
}
