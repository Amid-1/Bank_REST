package com.example.bankcards.entity.request;

import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "card_block_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardBlockRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private BankCard card;

    @ManyToOne(optional = false)
    @JoinColumn(name = "initiator_id", nullable = false)
    private AppUser initiator;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardBlockStatus status = CardBlockStatus.WAITING;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
