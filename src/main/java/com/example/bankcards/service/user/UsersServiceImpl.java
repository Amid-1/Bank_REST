package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.*;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.entity.user.UserRole;
import com.example.bankcards.repository.CardsRepository;   // ✅ добавили
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.service.user.mapper.UserMapper;
import com.example.bankcards.util.EmailNormalizer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;
    private final CardsRepository cardsRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse create(UserCreateRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Запрос на создание пользователя не должен быть null");
        }

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
                .accountNonLocked(true)
                .build();

        AppUser saved = usersRepository.save(user);
        return UserMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(Pageable pageable) {
        return usersRepository.findAll(pageable)
                .map(UserMapper::toDto);
    }

    @Override
    public UserResponse updateRole(UUID userId, UserRoleUpdateRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Запрос на смену роли не должен быть null");
        }

        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        user.setRole(req.role());

        AppUser saved = usersRepository.save(user);

        return UserMapper.toDto(saved);
    }

    @Override
    public UserResponse updateEnabled(UUID userId, UserEnabledUpdateRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Запрос на изменение enabled не должен быть null");
        }

        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        user.setEnabled(req.enabled());

        AppUser saved = usersRepository.save(user);

        return UserMapper.toDto(saved);
    }

    @Override
    public void resetPassword(UUID userId, AdminPasswordResetRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Запрос на смену пароля не должен быть null");
        }

        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));

        usersRepository.save(user);
    }

    @Override
    public void delete(UUID userId) {
        if (cardsRepository.existsByOwnerIdAndDeletedFalse(userId)) {
            throw new IllegalStateException("Нельзя удалить пользователя: есть активные карты");
        }

        if (cardsRepository.existsByOwnerId(userId)) {
            throw new IllegalStateException("Нельзя удалить пользователя: есть карты (в том числе удаленные)");
        }

        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        usersRepository.delete(user);
    }
}
