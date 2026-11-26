package com.miroyiro.backend.domain.artist.service;

import com.miroyiro.backend.domain.artist.dto.response.ArtistResponse;
import com.miroyiro.backend.domain.artist.entity.Artist;
import com.miroyiro.backend.domain.artist.repository.ArtistRepository;
import com.miroyiro.backend.domain.follow.repository.UserArtistFollowRepository;
import com.miroyiro.backend.global.error.BusinessException;
import com.miroyiro.backend.global.error.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final UserArtistFollowRepository followRepository;

    public ArtistService(
            ArtistRepository artistRepository,
            UserArtistFollowRepository followRepository
    ) {
        this.artistRepository = artistRepository;
        this.followRepository = followRepository;
    }

    // 아티스트 기본 정보 조회
    public ArtistResponse getArtist(
            Long artistId,
            Long userId
    ) {
        Artist artist = findArtist(artistId);

        long followerCount =
                followRepository.countByArtistId(artistId);

        Boolean isFollowed = null;

        if (userId != null) {
            isFollowed =
                    followRepository.existsByUserIdAndArtistId(
                            userId,
                            artistId
                    );
        }

        return ArtistResponse.from(
                artist,
                followerCount,
                isFollowed
        );
    }

    // 아티스트 팔로우
    @Transactional
    public FollowResult follow(
            Long artistId,
            Long userId
    ) {
        validateUser(userId);
        findArtist(artistId);

        int inserted =
                followRepository.insertFollowIfAbsent(
                        userId,
                        artistId
                );

        Long followId = followRepository
                .findByUserIdAndArtistId(userId, artistId)
                .orElseThrow(() ->
                        BusinessException.notFound("Follow")
                )
                .getId();

        return new FollowResult(
                followId,
                inserted == 1
        );
    }

    // 아티스트 언팔로우
    @Transactional
    public void unfollow(
            Long artistId,
            Long userId
    ) {
        validateUser(userId);
        findArtist(artistId);

        followRepository.deleteByUserIdAndArtistId(
                userId,
                artistId
        );
    }

    // 아티스트 존재 여부 확인
    private Artist findArtist(Long artistId) {
        return artistRepository.findById(artistId)
                .orElseThrow(() ->
                        BusinessException.notFound("Artist")
                );
    }

    // 로그인 여부 확인
    private void validateUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED
            );
        }
    }

    // 팔로우 처리 결과
    public record FollowResult(
            Long followId,
            boolean created
    ) {
    }
}
