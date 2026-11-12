package com.miroyiro.backend.domain.anime.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** miroyiro_main.anime_streaming — '어디서 보나'. UNIQUE(anime_id, platform, source). */
@Entity
@Table(name = "anime_streaming")
public class AnimeStreaming {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anime_id", nullable = false)
    private Long animeId;

    /** netflix | tving | watcha | wavve | disney_plus | coupang_play | laftel | aniplus_tv | etc */
    @Column(name = "platform", length = 20, nullable = false)
    private String platform;

    @Column(name = "url", length = 500)
    private String url;

    /** manual | tmdb | anilist | wikidata | laftel | aniplus */
    @Column(name = "source", length = 10, nullable = false)
    private String source;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    protected AnimeStreaming() {
    }

    public AnimeStreaming(Long animeId, String platform, String source) {
        this.animeId = animeId;
        this.platform = platform;
        this.source = source;
    }

    public Long getId() { return id; }
    public Long getAnimeId() { return animeId; }
    public String getPlatform() { return platform; }
    public String getUrl() { return url; }
    public String getSource() { return source; }
    public OffsetDateTime getVerifiedAt() { return verifiedAt; }

    public void setUrl(String url) { this.url = url; }
    public void setVerifiedAt(OffsetDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
