package com.example.bankcards.controller;

import com.example.bankcards.dto.user.*;
import com.example.bankcards.service.user.UsersService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Пользователи (админ)")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUsersController {

    private final UsersService usersService;

    @GetMapping
    public Page<UserResponse> getUsers(@ParameterObject Pageable pageable) {
        return usersService.getAll(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest req) {
        return usersService.create(req);
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateRole(@PathVariable UUID id,
                                   @Valid @RequestBody UserRoleUpdateRequest req) {
        return usersService.updateRole(id, req);
    }

    @PatchMapping("/{id}/enabled")
    public UserResponse updateEnabled(@PathVariable UUID id,
                                      @Valid @RequestBody UserEnabledUpdateRequest req) {
        return usersService.updateEnabled(id, req);
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable UUID id,
                              @Valid @RequestBody AdminPasswordResetRequest req) {
        usersService.resetPassword(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        usersService.delete(id);
    }
}
