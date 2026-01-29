package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.card.TransferRecord;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.TransferRecordsRepository;
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
    void transfer_success_balancesUpdated_andRecordCreated() {
        UUID userId = UUID.randomUUID();
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

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

        // порядок блокировок внутри сервиса определяется сравнением UUID (compareTo)
        when(cardsRepository.lockByIdAndOwnerId(eq(fromId), eq(userId))).thenReturn(Optional.of(from));
        when(cardsRepository.lockByIdAndOwnerId(eq(toId), eq(userId))).thenReturn(Optional.of(to));

        when(transferRecordsRepository.saveAndFlush(any(TransferRecord.class)))
                .thenAnswer(inv -> {
                    TransferRecord r = inv.getArgument(0);
                    r.setId(UUID.randomUUID());
                    r.setCreatedAt(LocalDateTime.now());
                    return r;
                });

        var resp = service.transfer(userId, new TransferRequest(fromId, toId, new BigDecimal("25.00")));

        assertThat(resp.amount()).isEqualByComparingTo("25.00");
        assertThat(resp.fromBalanceAfter()).isEqualByComparingTo("75.00");
        assertThat(resp.toBalanceAfter()).isEqualByComparingTo("35.00");

        verify(transferRecordsRepository, times(1)).saveAndFlush(any(TransferRecord.class));
    }

    @Test
    void transfer_insufficientFunds_returnsIllegalState() {
        UUID userId = UUID.randomUUID();
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

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
                .hasMessageContaining("Недостаточно средств");

        verify(transferRecordsRepository, never()).saveAndFlush(any());
    }

    @Test
    void transfer_fromEqualsTo_returnsIllegalArgument() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.transfer(userId, new TransferRequest(id, id, new BigDecimal("1.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("должны быть разными");
    }

    @Test
    void transfer_blockedOrExpired_returnsIllegalState() {
        UUID userId = UUID.randomUUID();
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        BankCard fromBlocked = BankCard.builder()
                .id(fromId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.BLOCKED)
                .expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("100.00"))
                .build();

        BankCard toActive = BankCard.builder()
                .id(toId)
                .owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("0.00"))
                .build();

        when(cardsRepository.lockByIdAndOwnerId(eq(fromId), eq(userId))).thenReturn(Optional.of(fromBlocked));
        when(cardsRepository.lockByIdAndOwnerId(eq(toId), eq(userId))).thenReturn(Optional.of(toActive));

        assertThatThrownBy(() -> service.transfer(userId, new TransferRequest(fromId, toId, new BigDecimal("1.00"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("не в статусе ACTIVE");
    }

    @Test
    void transfer_scaleMoreThan2_returnsIllegalArgument() {
        UUID userId = UUID.randomUUID();
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        assertThatThrownBy(() -> service.transfer(userId, new TransferRequest(fromId, toId, new BigDecimal("1.001"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не более 2 знаков");
    }

    @Test
    void transfer_locksInStableOrder_avoidsDeadlocks() {
        UUID userId = UUID.randomUUID();
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");

        BankCard ca = BankCard.builder()
                .id(a).owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE).expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("100.00"))
                .build();

        BankCard cb = BankCard.builder()
                .id(b).owner(AppUser.builder().id(userId).build())
                .status(BankCardStatus.ACTIVE).expirationDate(LocalDate.now().plusYears(1))
                .balance(new BigDecimal("0.00"))
                .build();

        when(cardsRepository.lockByIdAndOwnerId(eq(a), eq(userId))).thenReturn(Optional.of(ca));
        when(cardsRepository.lockByIdAndOwnerId(eq(b), eq(userId))).thenReturn(Optional.of(cb));
        when(transferRecordsRepository.saveAndFlush(any())).thenAnswer(inv -> {
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
