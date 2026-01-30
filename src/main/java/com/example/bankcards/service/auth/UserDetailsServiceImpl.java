package com.example.bankcards.service.auth;

import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        String email = EmailNormalizer.normalize(username);

        AppUser u = usersRepository.findByEmailLower(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new User(
                u.getEmail(),
                u.getPasswordHash(),
                u.isEnabled(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(u.getRole().name()))
        );
    }
}
