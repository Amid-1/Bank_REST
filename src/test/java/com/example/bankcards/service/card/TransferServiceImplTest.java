package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.card.TransferRecord;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.TransferRecordsRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock CardsRepository cardsRepository;
    @Mock TransferRecordsRepository transferRecordsRepository;

    @InjectMocks TransferServiceImpl service;

    @Test
    void transfer_success_balancesUpdated_andRecordSaved() {
        UUID userId = UUID.randomUUID();

        UUID fromId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID toId   = UUID.fromString("00000000-0000-0000-0000-000000000002");

        BankCard from = BankCard.builder()
                .id(fromId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("100.00"))
                .build();

        BankCard to = BankCard.builder()
                .id(toId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("10.00"))
                .build();

        when(cardsRepository.lockByIdAndOwnerId(eq(fromId), eq(userId))).thenReturn(Optional.of(from));
        when(cardsRepository.lockByIdAndOwnerId(eq(toId), eq(userId))).thenReturn(Optional.of(to));

        when(transferRecordsRepository.save(any(TransferRecord.class))).thenAnswer(inv -> {
            TransferRecord r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });

        var resp = service.transfer(userId, new TransferRequest(fromId, toId, new BigDecimal("25.00")));

        assertThat(from.getBalance()).isEqualByComparingTo("75.00");
        assertThat(to.getBalance()).isEqualByComparingTo("35.00");

        assertThat(resp.amount()).isEqualByComparingTo("25.00");
        assertThat(resp.fromBalanceAfter()).isEqualByComparingTo("75.00");
        assertThat(resp.toBalanceAfter()).isEqualByComparingTo("35.00");

        verify(transferRecordsRepository, times(1)).save(any(TransferRecord.class));
    }

    @Test
    void transfer_insufficientFunds_throwsIllegalState_withExactMessage() {
        UUID userId = UUID.randomUUID();

        UUID fromId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID toId   = UUID.fromString("00000000-0000-0000-0000-000000000002");

        BankCard from = BankCard.builder()
                .id(fromId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("10.00"))
                .build();

        BankCard to = BankCard.builder()
                .id(toId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("0.00"))
                .build();

        when(cardsRepository.lockByIdAndOwnerId(eq(fromId), eq(userId))).thenReturn(Optional.of(from));
        when(cardsRepository.lockByIdAndOwnerId(eq(toId), eq(userId))).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> service.transfer(userId, new TransferRequest(fromId, toId, new BigDecimal("25.00"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Недостаточно средств");

        verify(transferRecordsRepository, never()).save(any());
        assertThat(from.getBalance()).isEqualByComparingTo("10.00");
        assertThat(to.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void transfer_fromCardNotActive_throwsIllegalState_withExactMessage() {
        UUID userId = UUID.randomUUID();

        UUID fromId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID toId   = UUID.fromString("00000000-0000-0000-0000-000000000002");

        BankCard from = BankCard.builder()
                .id(fromId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.BLOCKED)
                .expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("100.00"))
                .build();

        BankCard to = BankCard.builder()
                .id(toId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("0.00"))
                .build();

        when(cardsRepository.lockByIdAndOwnerId(eq(fromId), eq(userId))).thenReturn(Optional.of(from));
        when(cardsRepository.lockByIdAndOwnerId(eq(toId), eq(userId))).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> service.transfer(userId, new TransferRequest(fromId, toId, new BigDecimal("1.00"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Карта не в статусе ACTIVE: " + fromId);

        verify(transferRecordsRepository, never()).save(any());
    }

    @Test
    void transfer_toCardExpired_throwsIllegalState_withExactMessage() {
        UUID userId = UUID.randomUUID();

        UUID fromId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID toId   = UUID.fromString("00000000-0000-0000-0000-000000000002");

        BankCard from = BankCard.builder()
                .id(fromId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("100.00"))
                .build();

        BankCard toExpired = BankCard.builder()
                .id(toId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().minusDays(1))
                .balance(new BigDecimal("0.00"))
                .build();

        when(cardsRepository.lockByIdAndOwnerId(eq(fromId), eq(userId))).thenReturn(Optional.of(from));
        when(cardsRepository.lockByIdAndOwnerId(eq(toId), eq(userId))).thenReturn(Optional.of(toExpired));

        assertThatThrownBy(() -> service.transfer(userId, new TransferRequest(fromId, toId, new BigDecimal("1.00"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Карта просрочена: " + toId);

        verify(transferRecordsRepository, never()).save(any());
    }

    @Test
    void transfer_toCardNotOwned_repoReturnsEmpty_throwsEntityNotFound_withExactMessage() {
        UUID userId = UUID.randomUUID();

        UUID fromId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID toId   = UUID.fromString("00000000-0000-0000-0000-000000000002");

        BankCard from = BankCard.builder()
                .id(fromId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("100.00"))
                .build();

        when(cardsRepository.lockByIdAndOwnerId(eq(fromId), eq(userId))).thenReturn(Optional.of(from));
        when(cardsRepository.lockByIdAndOwnerId(eq(toId), eq(userId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(userId, new TransferRequest(fromId, toId, new BigDecimal("1.00"))))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Карта не найдена: " + toId);

        verify(transferRecordsRepository, never()).save(any());
    }

    @Test
    void transfer_fromCardNotOwned_repoReturnsEmpty_throwsEntityNotFound_withExactMessage() {
        UUID userId = UUID.randomUUID();

        UUID fromId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID toId   = UUID.fromString("00000000-0000-0000-0000-000000000002");

        when(cardsRepository.lockByIdAndOwnerId(eq(fromId), eq(userId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(userId, new TransferRequest(fromId, toId, new BigDecimal("1.00"))))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Карта не найдена: " + fromId);

        verify(transferRecordsRepository, never()).save(any());
    }

    @Test
    void transfer_locksInStableOrder_avoidsDeadlocks() {
        UUID userId = UUID.randomUUID();

        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");

        BankCard ca = BankCard.builder()
                .id(a)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("50.00"))
                .build();

        BankCard cb = BankCard.builder()
                .id(b)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("50.00"))
                .build();

        when(cardsRepository.lockByIdAndOwnerId(eq(a), eq(userId))).thenReturn(Optional.of(ca));
        when(cardsRepository.lockByIdAndOwnerId(eq(b), eq(userId))).thenReturn(Optional.of(cb));
        when(transferRecordsRepository.save(any(TransferRecord.class)))
                .thenAnswer(inv -> {
                    TransferRecord r = inv.getArgument(0);
                    r.setId(UUID.randomUUID());
                    r.setCreatedAt(LocalDateTime.now());
                    return r;
                });

        service.transfer(userId, new TransferRequest(b, a, new BigDecimal("10.00")));

        InOrder inOrder = inOrder(cardsRepository);
        inOrder.verify(cardsRepository).lockByIdAndOwnerId(eq(a), eq(userId));
        inOrder.verify(cardsRepository).lockByIdAndOwnerId(eq(b), eq(userId));
    }
}
