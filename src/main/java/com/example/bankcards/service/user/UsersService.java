package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.UserCreateRequest;
import com.example.bankcards.dto.user.UserResponseDTO;

public interface UsersService {
    UserResponseDTO create(UserCreateRequest request);
}