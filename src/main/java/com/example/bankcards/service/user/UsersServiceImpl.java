package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.*;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.entity.user.UserRole;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.util.EmailNormalizer;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public UsersServiceImpl(UsersRepository usersRepository, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse create(UserCreateRequest req) {
        if (req == null) throw new IllegalArgumentException("Запрос на создание пользователя не должен быть null");

        String email = EmailNormalizer.normalize(req.email());

        if (usersRepository.existsByEmailLower(email)) {
            throw new IllegalStateException("Пользователь с email уже существует: " + email);
        }

        AppUser user = AppUser.builder()
                .name(req.name())
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(UserRole.ROLE_USER)
                .enabled(true)
                .build();

        usersRepository.save(user);
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(Pageable pageable) {
        return usersRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public void delete(UUID userId) {
        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));
        usersRepository.delete(user);
    }

    @Override
    public void resetPassword(UUID userId, AdminPasswordResetRequest req) {
        if (req == null) throw new IllegalArgumentException("request is null");

        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        usersRepository.save(user);
    }

    @Override
    public UserResponse updateEnabled(UUID userId, UserEnabledUpdateRequest req) {
        if (req == null) throw new IllegalArgumentException("request is null");

        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        user.setEnabled(req.enabled());
        usersRepository.save(user);
        return toResponse(user);
    }

    @Override
    public UserResponse updateRole(UUID userId, UserRoleUpdateRequest req) {
        if (req == null) throw new IllegalArgumentException("request is null");

        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        user.setRole(req.role());
        usersRepository.save(user);
        return toResponse(user);
    }

    private UserResponse toResponse(AppUser u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail());
    }
}
