package com.example.bankcards.entity.card;


import com.example.bankcards.entity.request.CardBlockStatus;
import com.example.bankcards.entity.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GenerationType;
import jakarta.persistence.;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


@Entity
@Table(name = "cards")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class BankCard {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "encrypted_card_number", unique = true)
    private String encryptedCardNumber;

    @Column(name = "masked_card_number")
    private String maskedCardNumber;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    private CardBlockStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser owner;

    private String hash;
}
