package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UsersService {
    UserResponse create(UserCreateRequest req);

    Page<UserResponse> getAll(Pageable pageable);
    void delete(UUID userId);

    void resetPassword(UUID userId, AdminPasswordResetRequest req);
    UserResponse updateEnabled(UUID userId, UserEnabledUpdateRequest req);
    UserResponse updateRole(UUID userId, UserRoleUpdateRequest req);
}
