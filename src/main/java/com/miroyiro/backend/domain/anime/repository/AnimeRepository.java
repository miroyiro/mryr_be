package com.miroyiro.backend.domain.anime.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.miroyiro.backend.domain.anime.entity.Anime;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimeRepository extends JpaRepository<Anime, Long> {

    Optional<Anime> findByAnilistId(Integer anilistId);

    Optional<Anime> findByAnissiaNo(Integer anissiaNo);

    List<Anime> findByUpdatedAtGreaterThanEqual(OffsetDateTime since);
}
