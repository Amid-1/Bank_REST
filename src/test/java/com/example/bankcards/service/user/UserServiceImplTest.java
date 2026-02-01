package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.AdminPasswordResetRequest;
import com.example.bankcards.dto.user.UserCreateRequest;
import com.example.bankcards.dto.user.UserEnabledUpdateRequest;
import com.example.bankcards.dto.user.UserRoleUpdateRequest;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.entity.user.UserRole;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.UsersRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UsersRepository usersRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CardsRepository cardsRepository;

    @InjectMocks UsersServiceImpl service;

    @InjectMocks UsersServiceImpl usersService;

    @Test
    void create_defaultRoleIsUser_andPasswordEncoded_andEmailNormalized() {
        String rawEmail = "  U@MAIL.RU  ";
        String normalizedEmail = "u@mail.ru";

        when(usersRepository.existsByEmailLower(normalizedEmail)).thenReturn(false);
        when(passwordEncoder.encode("very_strong_password")).thenReturn("ENC");
        when(usersRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.create(new UserCreateRequest("U", rawEmail, "very_strong_password"));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(usersRepository).save(captor.capture());
        AppUser saved = captor.getValue();

        assertThat(saved.getRole()).isEqualTo(UserRole.ROLE_USER);
        assertThat(saved.getPasswordHash()).isEqualTo("ENC");
        assertThat(saved.getEmail()).isEqualTo(normalizedEmail);

        assertThat(resp.email()).isEqualTo(normalizedEmail);
    }

    @Test
    void create_duplicateEmail_throwsIllegalState() {
        String normalizedEmail = "u@mail.ru";

        when(usersRepository.existsByEmailLower(normalizedEmail)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new UserCreateRequest("U", "U@MAIL.RU", "very_strong_password")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("уже существует");

        verify(usersRepository, never()).save(any());
    }

    @Test
    void resetPassword_changesPasswordHash() {
        UUID id = UUID.randomUUID();
        AppUser user = AppUser.builder()
                .id(id).name("U").email("u@mail.ru").passwordHash("OLD").role(UserRole.ROLE_USER)
                .enabled(true)
                .build();

        when(usersRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new_strong_password_123")).thenReturn("NEW_HASH");
        when(usersRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        service.resetPassword(id, new AdminPasswordResetRequest("new_strong_password_123"));

        assertThat(user.getPasswordHash()).isEqualTo("NEW_HASH");
        verify(usersRepository).save(user);
    }

    @Test
    void updateEnabled_togglesFlag() {
        UUID id = UUID.randomUUID();
        AppUser user = AppUser.builder()
                .id(id).name("U").email("u@mail.ru").passwordHash("x").role(UserRole.ROLE_USER)
                .enabled(true)
                .build();

        when(usersRepository.findById(id)).thenReturn(Optional.of(user));
        when(usersRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.updateEnabled(id, new UserEnabledUpdateRequest(false));

        assertThat(user.isEnabled()).isFalse();
        assertThat(resp.id()).isEqualTo(id);
        verify(usersRepository).save(user);
    }

    @Test
    void updateRole_setsRole() {
        UUID id = UUID.randomUUID();
        AppUser user = AppUser.builder()
                .id(id).name("U").email("u@mail.ru").passwordHash("x").role(UserRole.ROLE_USER)
                .enabled(true)
                .build();

        when(usersRepository.findById(id)).thenReturn(Optional.of(user));
        when(usersRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.updateRole(id, new UserRoleUpdateRequest(UserRole.ROLE_ADMIN));

        assertThat(user.getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        assertThat(resp.id()).isEqualTo(id);
        verify(usersRepository).save(user);
    }

    @Test
    void updateEnabled_userNotFound_throws404StyleEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(usersRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateEnabled(id, new UserEnabledUpdateRequest(false)))
                .isInstanceOf(EntityNotFoundException.class);

        verify(usersRepository, never()).save(any());
    }

    @Test
    void delete_whenUserHasActiveCards_throw409() {
        UUID userId = UUID.randomUUID();
        when(cardsRepository.existsByOwnerIdAndDeletedFalse(userId)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> usersService.delete(userId));
        assertEquals("Нельзя удалить пользователя: есть активные карты", ex.getMessage());

        verify(usersRepository, never()).delete(any());
    }

    @Test
    void delete_whenUserHasOnlyDeletedCards_throw409() {
        UUID userId = UUID.randomUUID();
        when(cardsRepository.existsByOwnerIdAndDeletedFalse(userId)).thenReturn(false);
        when(cardsRepository.existsByOwnerId(userId)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> usersService.delete(userId));
        assertEquals("Нельзя удалить пользователя: есть карты (в том числе удаленные)", ex.getMessage());

        verify(usersRepository, never()).delete(any());
    }

    @Test
    void delete_whenNoCards_deletesUser() {
        UUID userId = UUID.randomUUID();
        when(cardsRepository.existsByOwnerIdAndDeletedFalse(userId)).thenReturn(false);
        when(cardsRepository.existsByOwnerId(userId)).thenReturn(false);

        AppUser user = AppUser.builder().id(userId).email("a@b.c").name("A").passwordHash("x").role(UserRole.ROLE_USER).build();
        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));

        usersService.delete(userId);

        verify(usersRepository).delete(user);
    }
}
