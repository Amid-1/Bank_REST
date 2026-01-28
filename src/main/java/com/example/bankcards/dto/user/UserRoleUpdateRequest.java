package com.example.bankcards.dto.user;

import com.example.bankcards.entity.user.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на смену роли пользователя")
public record UserRoleUpdateRequest(
        @NotNull
        @Schema(description = "Новая роль", example = "ROLE_ADMIN")
        UserRole role
) {}
