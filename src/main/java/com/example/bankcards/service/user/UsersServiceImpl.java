package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.UserCreateRequest;
import com.example.bankcards.dto.user.UserResponseDTO;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.entity.user.UserRole;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.service.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDTO create(UserCreateRequest request) {
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
}