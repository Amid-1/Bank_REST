package com.example.bankcards.service.user.mapper;

import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.entity.user.AppUser;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserMapper {
    public UserResponse toDto(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}