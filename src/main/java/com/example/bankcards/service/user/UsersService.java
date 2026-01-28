package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UsersService {
    UserResponse create(UserCreateRequest request);
    Page<UserResponse> getAll(Pageable pageable);
    UserResponse updateRole(UUID userId, UserRoleUpdateRequest request);
    UserResponse updateEnabled(UUID userId, UserEnabledUpdateRequest request);
    void resetPassword(UUID userId, AdminPasswordResetRequest request);
    void delete(UUID userId);
}