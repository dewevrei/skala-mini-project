package com.dewevrei.aikanban.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dewevrei.aikanban.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByNicknameIgnoreCase(String nickname);
    boolean existsByGoogleId(String googleId);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByNicknameIgnoreCase(String nickname);
}
