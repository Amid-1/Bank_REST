package com.example.bankcards.service.user.mapper;

import com.example.bankcards.entity.user.AppUser;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserMapper {

    public UserResponseDTO toDto(AppUser user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}