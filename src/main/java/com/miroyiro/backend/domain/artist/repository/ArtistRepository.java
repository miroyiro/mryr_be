package com.miroyiro.backend.domain.artist.repository;

import java.time.OffsetDateTime;
import java.util.List;

import com.miroyiro.backend.domain.artist.entity.Artist;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    List<Artist> findByUpdatedAtGreaterThanEqual(OffsetDateTime since);
}
