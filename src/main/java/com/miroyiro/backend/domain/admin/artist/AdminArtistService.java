package com.miroyiro.backend.domain.admin.artist;

import com.miroyiro.backend.domain.artist.dto.request.ArtistCreateRequest;
import com.miroyiro.backend.domain.artist.dto.request.ArtistUpdateRequest;
import com.miroyiro.backend.domain.artist.dto.response.ArtistCreateResponse;
import com.miroyiro.backend.domain.artist.dto.response.ArtistUpdateResponse;
import com.miroyiro.backend.domain.artist.entity.Artist;
import com.miroyiro.backend.domain.artist.repository.ArtistRepository;
import com.miroyiro.backend.global.error.BusinessException;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminArtistService {

    private final ArtistRepository artistRepository;
    private final EntityManager entityManager;

    public AdminArtistService(
            ArtistRepository artistRepository,
            EntityManager entityManager
    ) {
        this.artistRepository = artistRepository;
        this.entityManager = entityManager;
    }

    // 아티스트 등록
    public ArtistCreateResponse create(
            ArtistCreateRequest request
    ) {
        Artist artist = new Artist(
                request.name(),
                request.nameEn(),
                request.nameJa(),
                request.category(),
                request.profileImage(),
                request.description(),
                request.debutDate(),
                request.officialUrl()
        );

        Artist saved =
                artistRepository.saveAndFlush(artist);

        entityManager.refresh(saved);

        return new ArtistCreateResponse(
                saved.getId(),
                saved.getCreatedAt()
        );
    }

    // 아티스트 수정
    public ArtistUpdateResponse update(
            Long artistId,
            ArtistUpdateRequest request
    ) {
        Artist artist =
                artistRepository.findById(artistId)
                        .orElseThrow(() ->
                                BusinessException.notFound("Artist")
                        );

        // 요청에 포함된 필드만 수정
        if (request.hasField("name")) {
            artist.updateName(request.name());
        }

        if (request.hasField("name_en")) {
            artist.updateNameEn(request.nameEn());
        }

        if (request.hasField("name_ja")) {
            artist.updateNameJa(request.nameJa());
        }

        if (request.hasField("profile_image")) {
            artist.updateProfileImage(request.profileImage());
        }

        if (request.hasField("description")) {
            artist.updateDescription(request.description());
        }

        if (request.hasField("debut_date")) {
            artist.updateDebutDate(request.debutDate());
        }

        if (request.hasField("official_url")) {
            artist.updateOfficialUrl(request.officialUrl());
        }

        entityManager.flush();
        entityManager.refresh(artist);

        return new ArtistUpdateResponse(
                artist.getId(),
                artist.getUpdatedAt()
        );
    }
}
