package com.miroyiro.backend.domain.follow.repository;

import java.util.List;
import java.util.Optional;

import com.miroyiro.backend.domain.follow.entity.UserArtistFollow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserArtistFollowRepository
        extends JpaRepository<UserArtistFollow, Long> {

    Optional<UserArtistFollow> findByUserIdAndArtistId(
            Long userId,
            Long artistId
    );

    boolean existsByUserIdAndArtistId(
            Long userId,
            Long artistId
    );

    long countByArtistId(Long artistId);

    @Modifying
    @Query("""
            DELETE FROM UserArtistFollow f
            WHERE f.userId = :userId
            AND f.artistId = :artistId
            """)
    int deleteByUserIdAndArtistId(
            @Param("userId") Long userId,
            @Param("artistId") Long artistId
    );

    List<UserArtistFollow> findByUserIdAndMatchStatus(
            Long userId,
            String matchStatus
    );

    @Modifying
    @Query(value = """
        INSERT INTO miroyiro_main.user_artist_follow
            (user_id, artist_id, match_status, resolved_at)
        VALUES
            (:userId, :artistId, 'matched', now())
        ON CONFLICT (user_id, artist_id) DO NOTHING
        """, nativeQuery = true)
    int insertFollowIfAbsent(
            @Param("userId") Long userId,
            @Param("artistId") Long artistId
    );
}
