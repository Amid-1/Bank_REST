package com.example.bankcards.repository;

import com.example.bankcards.entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий пользователей.
 * Используется для аутентификации (по email) и проверки уникальности email.
 */
public interface UsersRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
}