package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.CardBlockRequestCreate;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.request.CardBlockRequest;
import com.example.bankcards.entity.request.CardBlockStatus;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.repository.CardBlockRequestsRepository;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.UsersRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardBlockRequestsServiceImplTest {

    @Mock CardBlockRequestsRepository blockRequestsRepository;
    @Mock CardsRepository cardsRepository;
    @Mock UsersRepository usersRepository;

    @InjectMocks CardBlockRequestsServiceImpl service;

    @Test
    void create_waiting_ok() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        BankCard card = BankCard.builder()
                .id(cardId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .deleted(false)
                .build();

        when(cardsRepository.findByIdAndOwnerIdAndDeletedFalse(cardId, userId)).thenReturn(Optional.of(card));
        when(blockRequestsRepository.existsByCard_IdAndStatus(cardId, CardBlockStatus.WAITING)).thenReturn(false);
        when(usersRepository.findById(userId)).thenReturn(Optional.of(AppUser.builder().id(userId).build()));

        when(blockRequestsRepository.save(any(CardBlockRequest.class))).thenAnswer(inv -> {
            CardBlockRequest r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        var resp = service.create(userId, new CardBlockRequestCreate(cardId, "Потеряна"));

        assertThat(resp.cardId()).isEqualTo(cardId);
        assertThat(resp.initiatorId()).isEqualTo(userId);
        assertThat(resp.status()).isEqualTo(CardBlockStatus.WAITING);
        assertThat(resp.reason()).isEqualTo("Потеряна");

        verify(blockRequestsRepository).save(any(CardBlockRequest.class));
    }

    @Test
    void create_cardNotOwnedOrDeleted_throwsAccessDenied() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        when(cardsRepository.findByIdAndOwnerIdAndDeletedFalse(cardId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(userId, new CardBlockRequestCreate(cardId, "Потеряна")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Карта не найдена");

        verify(blockRequestsRepository, never()).save(any());
    }

    @Test
    void create_cardNotActive_throwsIllegalState() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        BankCard card = BankCard.builder()
                .id(cardId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.BLOCKED)
                .expirationDate(LocalDate.now().plusYears(1))
                .deleted(false)
                .build();

        when(cardsRepository.findByIdAndOwnerIdAndDeletedFalse(cardId, userId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.create(userId, new CardBlockRequestCreate(cardId, "Потеряна")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("не находится в статусе ACTIVE");

        verify(blockRequestsRepository, never()).save(any());
    }

    @Test
    void create_cardExpired_throwsIllegalState() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        BankCard card = BankCard.builder()
                .id(cardId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().minusDays(1))
                .deleted(false)
                .build();

        when(cardsRepository.findByIdAndOwnerIdAndDeletedFalse(cardId, userId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.create(userId, new CardBlockRequestCreate(cardId, "Потеряна")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Срок действия карты истек");

        verify(blockRequestsRepository, never()).save(any());
    }

    @Test
    void create_duplicateWaiting_throwsIllegalState() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        BankCard card = BankCard.builder()
                .id(cardId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .deleted(false)
                .build();

        when(cardsRepository.findByIdAndOwnerIdAndDeletedFalse(cardId, userId)).thenReturn(Optional.of(card));
        when(blockRequestsRepository.existsByCard_IdAndStatus(cardId, CardBlockStatus.WAITING)).thenReturn(true);

        assertThatThrownBy(() -> service.create(userId, new CardBlockRequestCreate(cardId, "Потеряна")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("уже существует");

        verify(blockRequestsRepository, never()).save(any());
        verify(usersRepository, never()).findById(any());
    }

    @Test
    void approve_blocksCard_andApprovesRequest() {
        UUID reqId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        BankCard card = BankCard.builder()
                .id(cardId)
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .owner(AppUser.builder().id(UUID.randomUUID()).build())
                .deleted(false)
                .build();

        CardBlockRequest req = CardBlockRequest.builder()
                .id(reqId)
                .card(card)
                .initiator(AppUser.builder().id(UUID.randomUUID()).build())
                .status(CardBlockStatus.WAITING)
                .build();

        when(blockRequestsRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(cardsRepository.findByIdAndDeletedFalse(cardId)).thenReturn(Optional.of(card));

        service.approve(reqId);

        assertThat(card.getStatus()).isEqualTo(BankCardStatus.BLOCKED);
        assertThat(req.getStatus()).isEqualTo(CardBlockStatus.APPROVED);
    }

    @Test
    void approve_requestNotFound_throwsEntityNotFound() {
        UUID reqId = UUID.randomUUID();
        when(blockRequestsRepository.findById(reqId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(reqId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Заявка не найдена");
    }

    @Test
    void approve_cardNotFoundOrDeleted_throwsEntityNotFound() {
        UUID reqId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        CardBlockRequest req = CardBlockRequest.builder()
                .id(reqId)
                .card(BankCard.builder().id(cardId).build())
                .initiator(AppUser.builder().id(UUID.randomUUID()).build())
                .status(CardBlockStatus.WAITING)
                .build();

        when(blockRequestsRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(cardsRepository.findByIdAndDeletedFalse(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(reqId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Карта не найдена");
    }

    @Test
    void reject_setsRejected() {
        UUID reqId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        CardBlockRequest req = CardBlockRequest.builder()
                .id(reqId)
                .card(BankCard.builder().id(cardId).build())
                .initiator(AppUser.builder().id(UUID.randomUUID()).build())
                .status(CardBlockStatus.WAITING)
                .build();

        when(blockRequestsRepository.findById(reqId)).thenReturn(Optional.of(req));

        service.reject(reqId);

        assertThat(req.getStatus()).isEqualTo(CardBlockStatus.REJECTED);
    }
}
