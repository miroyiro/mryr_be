package com.miroyiro.backend.domain.auth.repository;

import com.miroyiro.backend.domain.auth.entity.UserRefreshToken;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository
        extends JpaRepository<UserRefreshToken, Long> {

    // Refresh Token 해시로 조회
    Optional<UserRefreshToken> findByTokenHash(
            String tokenHash
    );

    // Refresh Token 재발급 시 동시 요청 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT t
        FROM UserRefreshToken t
        JOIN FETCH t.user
        WHERE t.tokenHash = :tokenHash
    """)
    Optional<UserRefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    // 특정 사용자의 폐기되지 않은 Refresh Token 조회
    List<UserRefreshToken> findByUserIdAndRevokedAtIsNull(
            Long userId
    );
}
