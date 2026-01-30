package com.example.bankcards.service.auth;

import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsersRepository usersRepository;

    @Override
    @Transactional(readOnly = true)
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) {
        String email = EmailNormalizer.normalize(username);

        return usersRepository.findByEmailLower(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
