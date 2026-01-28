package com.example.bankcards.config;

import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.entity.user.UserRole;
import com.example.bankcards.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminBootstrapConfiguration {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner adminBootstrap(
            @Value("${app.admin.email}") String email,
            @Value("${app.admin.name}") String name,
            @Value("${app.admin.password}") String password
    ) {
        return args -> {
            if (usersRepository.existsByEmail(email)) return;

            AppUser admin = AppUser.builder()
                    .email(email)
                    .name(name)
                    .passwordHash(passwordEncoder.encode(password))
                    .role(UserRole.ROLE_ADMIN)
                    .build();

            usersRepository.save(admin);
        };
    }
}
