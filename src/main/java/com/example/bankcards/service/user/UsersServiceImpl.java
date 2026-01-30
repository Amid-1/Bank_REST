package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.AdminPasswordResetRequest;
import com.example.bankcards.dto.user.UserCreateRequest;
import com.example.bankcards.dto.user.UserEnabledUpdateRequest;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.dto.user.UserRoleUpdateRequest;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.entity.user.UserRole;
import com.example.bankcards.repository.UsersRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UsersServiceImpl {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public UsersServiceImpl(UsersRepository usersRepository, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse create(UserCreateRequest req) {
        if (usersRepository.existsByEmail(req.email())) {
            throw new IllegalStateException("Пользователь с email уже существует: " + req.email());
        }

        AppUser user = AppUser.builder()
                .name(req.name())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(UserRole.ROLE_USER)
                .enabled(true)
                .build();

        AppUser saved = usersRepository.save(user);
        return toResponse(saved);
    }

    public void resetPassword(UUID userId, AdminPasswordResetRequest req) {
        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        usersRepository.save(user);
    }

    public UserResponse updateEnabled(UUID userId, UserEnabledUpdateRequest req) {
        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        user.setEnabled(req.enabled());
        AppUser saved = usersRepository.save(user);
        return toResponse(saved);
    }

    public UserResponse updateRole(UUID userId, UserRoleUpdateRequest req) {
        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        user.setRole(req.role());
        AppUser saved = usersRepository.save(user);
        return toResponse(saved);
    }

    private UserResponse toResponse(AppUser u) {
        return new UserResponse(
                u.getId(),
                u.getName(),
                u.getEmail()
        );
    }
}
