package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.util.PanEncryptor;
import com.example.bankcards.util.PepperHashEncoder;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardsServiceImplTest {

    @Mock CardsRepository cardsRepository;
    @Mock UsersRepository usersRepository;
    @Mock PepperHashEncoder hashEncoder;
    @Mock PanEncryptor panEncryptor;

    @InjectMocks CardsServiceImpl service;

    @Test
    void createCard_fillsMaskHashEncryption_andActiveStatus() {
        UUID ownerId = UUID.randomUUID();
        AppUser owner = AppUser.builder()
                .id(ownerId)
                .name("U")
                .email("u@mail.ru")
                .passwordHash("x")
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

        assertThat(resp.maskedCardNumber()).isEqualTo("**** **** **** 1111");
        assertThat(resp.status()).isEqualTo(BankCardStatus.ACTIVE);

        ArgumentCaptor<BankCard> cardCaptor = ArgumentCaptor.forClass(BankCard.class);
        verify(cardsRepository).save(cardCaptor.capture());
        BankCard saved = cardCaptor.getValue();

        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.getStatus()).isEqualTo(BankCardStatus.ACTIVE);
        assertThat(saved.getExpirationDate()).isEqualTo(req.expirationDate());
        assertThat(saved.getPanHash()).isEqualTo("hash-hex");
        assertThat(saved.getEncryptedCardNumber()).isEqualTo("enc-pan");
        assertThat(saved.getMaskedCardNumber()).isEqualTo("**** **** **** 1111");

        ArgumentCaptor<String> panCaptor = ArgumentCaptor.forClass(String.class);
        verify(hashEncoder).sha256Hex(panCaptor.capture());
        assertThat(panCaptor.getValue()).isEqualTo("4111111111111111");

        verify(panEncryptor).encrypt("4111111111111111");
    }

    @Test
    void createCard_duplicatePanHash_throwsIllegalState_andDoesNotSave() {
        UUID ownerId = UUID.randomUUID();

        when(usersRepository.findById(ownerId))
                .thenReturn(Optional.of(AppUser.builder().id(ownerId).build()));

        when(hashEncoder.sha256Hex(any())).thenReturn("hash-hex");
        when(cardsRepository.existsByPanHash("hash-hex")).thenReturn(true);

        var req = new CardCreateRequest("4111111111111111", LocalDate.now().plusYears(2), ownerId);

        assertThatThrownBy(() -> service.createCard(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Карта с таким номером уже существует");

        verify(cardsRepository).existsByPanHash("hash-hex");
        verify(cardsRepository, never()).save(any());
        verify(panEncryptor, never()).encrypt(any());
    }

    @Test
    void createCard_invalidPanLength_throwsIllegalArgument() {
        UUID ownerId = UUID.randomUUID();
        when(usersRepository.findById(ownerId))
                .thenReturn(Optional.of(AppUser.builder().id(ownerId).build()));

        var req = new CardCreateRequest("1234", LocalDate.now().plusYears(1), ownerId);

        assertThatThrownBy(() -> service.createCard(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Некорректная длина номера карты");

        verify(hashEncoder, never()).sha256Hex(any());
        verify(cardsRepository, never()).save(any());
        verify(panEncryptor, never()).encrypt(any());
    }

    @Test
    void createCard_userNotFound_throwsEntityNotFound_withExactMessage() {
        UUID ownerId = UUID.randomUUID();
        when(usersRepository.findById(ownerId)).thenReturn(Optional.empty());

        var req = new CardCreateRequest("4111111111111111", LocalDate.now().plusYears(1), ownerId);

        assertThatThrownBy(() -> service.createCard(req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Пользователь не найден: " + ownerId);

        verify(hashEncoder, never()).sha256Hex(any());
        verify(cardsRepository, never()).save(any());
        verify(panEncryptor, never()).encrypt(any());
    }

    @Test
    void activateCard_expired_throwsIllegalState_withExactMessage() {
        UUID cardId = UUID.randomUUID();

        BankCard card = BankCard.builder()
                .id(cardId)
                .expirationDate(LocalDate.now().minusDays(1))
                .status(BankCardStatus.BLOCKED)
                .owner(AppUser.builder().id(UUID.randomUUID()).build())
                .deleted(false)
                .build();

        when(cardsRepository.findByIdAndDeletedFalse(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.activateCard(cardId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Нельзя активировать просроченную карту");

        verify(cardsRepository, never()).save(any());
    }

    @Test
    void getMyCards_invalidLast4_throwsIllegalArgument_withExactMessage() {
        assertThatThrownBy(() ->
                service.getMyCards(UUID.randomUUID(), null, "12ab", PageRequest.of(0, 10))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("last4 должен состоять ровно из 4 цифр");
    }
}
