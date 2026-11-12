package com.miroyiro.backend.internal.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.miroyiro.backend.domain.anime.entity.Anime;
import com.miroyiro.backend.domain.anime.entity.AnimeStreaming;
import com.miroyiro.backend.domain.anime.repository.AnimeRepository;
import com.miroyiro.backend.domain.anime.repository.AnimeStreamingRepository;
import com.miroyiro.backend.domain.artist.entity.Artist;
import com.miroyiro.backend.domain.artist.repository.ArtistRepository;
import com.miroyiro.backend.global.error.BusinessException;
import com.miroyiro.backend.global.error.ErrorCode;
import com.miroyiro.backend.internal.dto.AnimeUpsertRequest;
import com.miroyiro.backend.internal.dto.CatalogName;
import com.miroyiro.backend.internal.dto.IdResponse;
import com.miroyiro.backend.internal.dto.StreamingUpsertRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 카탈로그 보강 (scrap → backend). processed_item 검수를 거치지 않는 사실정보 흐름. */
@Service
public class InternalCatalogService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 직접 확인한 출처 (딥링크 있음) → verified_at 채움. tmdb/anilist 는 NULL */
    private static final Set<String> VERIFIED_SOURCES = Set.of("laftel", "aniplus", "manual");

    private final AnimeRepository animes;
    private final AnimeStreamingRepository streamings;
    private final ArtistRepository artists;

    public InternalCatalogService(AnimeRepository animes, AnimeStreamingRepository streamings,
                                  ArtistRepository artists) {
        this.animes = animes;
        this.streamings = streamings;
        this.artists = artists;
    }

    @Transactional
    public IdResponse upsertAnime(AnimeUpsertRequest r) {
        if (r.anilistId() == null && r.anissiaNo() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "anilist_id 또는 anissia_no 필요");
        }
        Anime a = null;
        if (r.anilistId() != null) {
            a = animes.findByAnilistId(r.anilistId()).orElse(null);
        }
        if (a == null && r.anissiaNo() != null) {
            a = animes.findByAnissiaNo(r.anissiaNo()).orElse(null);
        }
        boolean created = a == null;
        if (created) {
            a = new Anime(r.titleRomaji());
        }
        // null 은 '모름' → 기존 값 유지 (소스마다 아는 필드가 다름: AniList=영/일, 애니시아=한국어)
        setIfPresent(r.anilistId(), a::setAnilistId);
        setIfPresent(r.anissiaNo(), a::setAnissiaNo);
        setIfPresent(r.tmdbId(), a::setTmdbId);
        setIfPresent(r.titleRomaji(), a::setTitleRomaji);
        setIfPresent(r.titleKo(), a::setTitleKo);
        setIfPresent(r.titleNative(), a::setTitleNative);
        setIfPresent(r.titleEn(), a::setTitleEn);
        setIfPresent(r.season(), a::setSeason);
        setIfPresent(r.seasonYear(), a::setSeasonYear);
        setIfPresent(r.status(), a::setStatus);
        setIfPresent(r.posterUrl(), a::setPosterUrl);
        a.setSyncedAt(OffsetDateTime.now(KST));
        return new IdResponse(animes.save(a).getId(), created);
    }

    @Transactional
    public int upsertStreaming(StreamingUpsertRequest r) {
        Anime anime = animes.findById(r.animeId()).orElseThrow(() -> BusinessException.notFound("anime"));
        OffsetDateTime now = OffsetDateTime.now(KST);
        int count = 0;
        for (StreamingUpsertRequest.Link link : r.links()) {
            AnimeStreaming s = streamings
                    .findByAnimeIdAndPlatformAndSource(anime.getId(), link.platform(), link.source())
                    .orElseGet(() -> new AnimeStreaming(anime.getId(), link.platform(), link.source()));
            if (link.url() != null) {
                s.setUrl(link.url());
            }
            if (VERIFIED_SOURCES.contains(link.source())) {
                s.setVerifiedAt(now);
            }
            streamings.save(s);
            if ("laftel".equals(link.platform()) && link.externalId() != null) {
                anime.setLaftelId(link.externalId());
            }
            count++;
        }
        return count;
    }

    /** name_matcher 용 이름 목록. since 가 있으면 그 이후 바뀐 것만 (scrap 이 10분 캐시). */
    @Transactional(readOnly = true)
    public List<CatalogName> names(OffsetDateTime since) {
        List<Anime> animeRows = since == null ? animes.findAll() : animes.findByUpdatedAtGreaterThanEqual(since);
        List<Artist> artistRows = since == null ? artists.findAll() : artists.findByUpdatedAtGreaterThanEqual(since);
        List<CatalogName> out = new ArrayList<>();
        for (Anime a : animeRows) {
            for (String n : distinct(a.getTitleKo(), a.getTitleRomaji(), a.getTitleNative(), a.getTitleEn())) {
                out.add(new CatalogName("anime", a.getId(), n));
            }
        }
        for (Artist a : artistRows) {
            for (String n : distinct(a.getName(), a.getNameEn(), a.getNameJa())) {
                out.add(new CatalogName("artist", a.getId(), n));
            }
        }
        return out;
    }

    private static Set<String> distinct(String... names) {
        Set<String> set = new LinkedHashSet<>();
        for (String n : names) {
            if (n != null && !n.isBlank()) {
                set.add(n.strip());
            }
        }
        return set;
    }

    private static <T> void setIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
