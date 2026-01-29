package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.util.PanEncryptor;
import com.example.bankcards.util.PepperHashEncoder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock CardsRepository cardsRepository;
    @Mock UsersRepository usersRepository;
    @Mock PepperHashEncoder hashEncoder;
    @Mock PanEncryptor panEncryptor;

    @InjectMocks CardsServiceImpl service;

    @Test
    void createCard_fillsMaskHashEncryption_andActiveStatus() {
        UUID ownerId = UUID.randomUUID();
        AppUser owner = AppUser.builder()
                .id(ownerId).name("U").email("u@mail.ru").passwordHash("x")
                .build();

        when(usersRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(hashEncoder.sha256Hex(any())).thenReturn("hash-hex");
        when(cardsRepository.existsByPanHash("hash-hex")).thenReturn(false);
        when(panEncryptor.encrypt(any())).thenReturn("enc-pan");

        when(cardsRepository.save(any(BankCard.class))).thenAnswer(inv -> {
            BankCard c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        var req = new CardCreateRequest("4111 1111 1111 1111", LocalDate.now().plusYears(2), ownerId);
        var resp = service.createCard(req);

        assertThat(resp.maskedCardNumber()).endsWith("1111");
        assertThat(resp.status()).isEqualTo(BankCardStatus.ACTIVE);

        ArgumentCaptor<BankCard> captor = ArgumentCaptor.forClass(BankCard.class);
        verify(cardsRepository).save(captor.capture());
        BankCard saved = captor.getValue();

        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.getPanHash()).isEqualTo("hash-hex");
        assertThat(saved.getEncryptedCardNumber()).isEqualTo("enc-pan");
        assertThat(saved.getMaskedCardNumber()).isEqualTo("**** **** **** 1111");
    }

    @Test
    void createCard_duplicatePanHash_throwsIllegalState() {
        UUID ownerId = UUID.randomUUID();
        when(usersRepository.findById(ownerId))
                .thenReturn(Optional.of(AppUser.builder().id(ownerId).name("U").email("u@mail.ru").passwordHash("x").build()));

        when(hashEncoder.sha256Hex(any())).thenReturn("hash-hex");
        when(cardsRepository.existsByPanHash("hash-hex")).thenReturn(true);

        var req = new CardCreateRequest("4111111111111111", LocalDate.now().plusYears(2), ownerId);

        assertThatThrownBy(() -> service.createCard(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("дубликат panHash");
    }

    @Test
    void activateCard_expired_throwsIllegalState() {
        UUID cardId = UUID.randomUUID();
        BankCard card = BankCard.builder()
                .id(cardId)
                .expirationDate(LocalDate.now().minusDays(1))
                .status(BankCardStatus.BLOCKED)
                .owner(AppUser.builder().id(UUID.randomUUID()).build())
                .build();

        when(cardsRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.activateCard(cardId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Нельзя активировать просроченную карту");
    }

    @Test
    void createCard_userNotFound_throwsEntityNotFound() {
        UUID ownerId = UUID.randomUUID();
        when(usersRepository.findById(ownerId)).thenReturn(Optional.empty());

        var req = new CardCreateRequest("4111111111111111", LocalDate.now().plusYears(1), ownerId);

        assertThatThrownBy(() -> service.createCard(req))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getMyCards_invalidLast4_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                service.getMyCards(UUID.randomUUID(), null, "12ab", org.springframework.data.domain.PageRequest.of(0, 10))
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last4 должен состоять ровно из 4 цифр");
    }

    @Disabled("Enable after implementing effective EXPIRED filter (expirationDate < today) in specifications/service")
    @Test
    void searchCards_statusExpired_shouldReturnCardsWithPastExpirationDate() {

    }
}
