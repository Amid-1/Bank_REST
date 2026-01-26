package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.UserCreateRequest;
import com.example.bankcards.dto.user.UserResponse;

public interface UsersService {
    UserResponse create(UserCreateRequest request);
}