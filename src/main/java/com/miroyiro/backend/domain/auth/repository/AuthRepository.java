package com.miroyiro.backend.domain.auth.repository;

import com.miroyiro.backend.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회 (이메일 로그인)
    Optional<User> findByEmailAndDeletedAtIsNull(
            String email
    );

    // 이메일 중복 확인 (회원가입)
    boolean existsByEmail(String email);

    // 닉네임 중복 확인 (회원가입)
    boolean existsByNickname(String nickname);

    // 사용자 ID로 조회 (내 정보, JWT 인증)
    Optional<User> findByIdAndDeletedAtIsNull(
            Long id
    );

    // OAuth 제공자 + OAuth ID로 조회 (소셜 로그인)
    Optional<User> findByOauthProviderAndOauthIdAndDeletedAtIsNull(
            String oauthProvider,
            String oauthId
    );
}
