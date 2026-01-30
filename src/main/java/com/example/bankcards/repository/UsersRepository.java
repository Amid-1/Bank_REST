package com.example.bankcards.repository;

import com.example.bankcards.entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsersRepository extends JpaRepository<AppUser, UUID> {

    @Query("select (count(u) > 0) from AppUser u where lower(u.email) = lower(:email)")
    boolean existsByEmailLower(@Param("email") String email);

    @Query("select u from AppUser u where lower(u.email) = lower(:email)")
    Optional<AppUser> findByEmailLower(@Param("email") String email);
}
