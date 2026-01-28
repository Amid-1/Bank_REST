package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.*;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.entity.user.UserRole;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.service.user.mapper.UserMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse create(UserCreateRequest request) {
        if (usersRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("User with email already exists");
        }

        AppUser user = AppUser.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.ROLE_USER)
                .build();

        AppUser saved = usersRepository.save(user);
        return UserMapper.toDto(saved);
    }

    @Override
    public Page<UserResponse> getAll(Pageable pageable) {
        return usersRepository.findAll(pageable).map(UserMapper::toDto);
    }

    @Override
    @Transactional
    public UserResponse updateRole(UUID userId, UserRoleUpdateRequest request) {
        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        user.setRole(request.role());
        return UserMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserResponse updateEnabled(UUID userId, UserEnabledUpdateRequest request) {
        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        user.setEnabled(request.enabled());
        return UserMapper.toDto(user);
    }

    @Override
    @Transactional
    public void resetPassword(UUID userId, AdminPasswordResetRequest request) {
        AppUser user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    @Override
    @Transactional
    public void delete(UUID userId) {
        if (!usersRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found: " + userId);
        }
        usersRepository.deleteById(userId);
    }
}