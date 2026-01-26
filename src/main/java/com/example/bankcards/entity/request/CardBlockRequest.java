package com.example.bankcards.entity.request;

import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.user.AppUser;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.util.UUID;

@Entity
@Table(name = "card_block_request")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class CardBlockRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "card_id")
    private BankCard card;

    @ManyToOne
    @JoinColumn(name = "initiator_id")
    private AppUser initiator;

    @Enumerated(EnumType.STRING)
    private CardBlockStatus state;
}
