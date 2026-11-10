package com.miroyiro.backend.domain.anime.repository;

import java.util.Optional;

import com.miroyiro.backend.domain.anime.entity.AnimeStreaming;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimeStreamingRepository extends JpaRepository<AnimeStreaming, Long> {

    Optional<AnimeStreaming> findByAnimeIdAndPlatformAndSource(Long animeId, String platform, String source);
}
