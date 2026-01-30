package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.AdminPasswordResetRequest;
import com.example.bankcards.dto.user.UserCreateRequest;
import com.example.bankcards.dto.user.UserEnabledUpdateRequest;
import com.example.bankcards.dto.user.UserRoleUpdateRequest;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.entity.user.UserRole;
import com.example.bankcards.repository.UsersRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.EntityNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UsersRepository usersRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UsersServiceImpl service;

    @Test
    void create_defaultRoleIsUser_andPasswordEncoded() {
        when(usersRepository.existsByEmail("u@mail.ru")).thenReturn(false);
        when(passwordEncoder.encode("very_strong_password")).thenReturn("ENC");

        when(usersRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            return u;
        });

        var resp = service.create(new UserCreateRequest("U", "u@mail.ru", "very_strong_password"));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(usersRepository).save(captor.capture());
        AppUser saved = captor.getValue();

        assertThat(saved.getRole()).isEqualTo(UserRole.ROLE_USER);
        assertThat(saved.getPasswordHash()).isEqualTo("ENC");
        assertThat(resp.email()).isEqualTo("u@mail.ru");
    }

    @Test
    void create_duplicateEmail_throwsIllegalState() {
        when(usersRepository.existsByEmail("u@mail.ru")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new UserCreateRequest("U", "u@mail.ru", "very_strong_password")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    void resetPassword_changesPasswordHash() {
        UUID id = UUID.randomUUID();
        AppUser user = AppUser.builder()
                .id(id).name("U").email("u@mail.ru").passwordHash("OLD").role(UserRole.ROLE_USER)
                .build();

        when(usersRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new_strong_password_123")).thenReturn("NEW_HASH");

        service.resetPassword(id, new AdminPasswordResetRequest("new_strong_password_123"));

        assertThat(user.getPasswordHash()).isEqualTo("NEW_HASH");
    }

    @Test
    void updateEnabled_togglesFlag() {
        UUID id = UUID.randomUUID();
        AppUser user = AppUser.builder()
                .id(id).name("U").email("u@mail.ru").passwordHash("x").role(UserRole.ROLE_USER)
                .build();

        when(usersRepository.findById(id)).thenReturn(Optional.of(user));

        var resp = service.updateEnabled(id, new UserEnabledUpdateRequest(false));

        assertThat(user.isEnabled()).isFalse();
        assertThat(resp.id()).isEqualTo(id);
    }

    @Test
    void updateRole_setsRole() {
        UUID id = UUID.randomUUID();
        AppUser user = AppUser.builder()
                .id(id).name("U").email("u@mail.ru").passwordHash("x").role(UserRole.ROLE_USER)
                .build();

        when(usersRepository.findById(id)).thenReturn(Optional.of(user));

        var resp = service.updateRole(id, new UserRoleUpdateRequest(UserRole.ROLE_ADMIN));

        assertThat(user.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        assertThat(resp.id()).isEqualTo(id);
    }

    @Test
    void updateEnabled_userNotFound_throws404StyleEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(usersRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateEnabled(id, new UserEnabledUpdateRequest(false)))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
