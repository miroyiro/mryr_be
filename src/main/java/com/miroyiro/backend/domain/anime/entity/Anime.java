package com.miroyiro.backend.domain.anime.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * miroyiro_main.anime (ERD v1.2). 지금은 내부 API(카탈로그 upsert·이름 목록)에 필요한 컬럼만 매핑.
 * 나머지(genres JSONB, broadcast_time 등)는 /api/anime 만들 때 추가.
 * created_at / updated_at 은 DB(DEFAULT now() + 트리거)가 채움 → JPA 는 읽기만.
 */
@Entity
@Table(name = "anime")
public class Anime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anilist_id")
    private Integer anilistId;

    @Column(name = "anissia_no")
    private Integer anissiaNo;

    @Column(name = "tmdb_id")
    private Integer tmdbId;

    @Column(name = "laftel_id", length = 30)
    private String laftelId;

    @Column(name = "title_ko", length = 200)
    private String titleKo;

    @Column(name = "title_romaji", length = 200, nullable = false)
    private String titleRomaji;

    @Column(name = "title_native", length = 200)
    private String titleNative;

    @Column(name = "title_en", length = 200)
    private String titleEn;

    @Column(name = "season", length = 10)
    private String season;

    @Column(name = "season_year")
    private Short seasonYear;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "synced_at")
    private OffsetDateTime syncedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Anime() {
    }

    public Anime(String titleRomaji) {
        this.titleRomaji = titleRomaji;
    }

    public Long getId() { return id; }
    public Integer getAnilistId() { return anilistId; }
    public Integer getAnissiaNo() { return anissiaNo; }
    public Integer getTmdbId() { return tmdbId; }
    public String getLaftelId() { return laftelId; }
    public String getTitleKo() { return titleKo; }
    public String getTitleRomaji() { return titleRomaji; }
    public String getTitleNative() { return titleNative; }
    public String getTitleEn() { return titleEn; }
    public String getSeason() { return season; }
    public Short getSeasonYear() { return seasonYear; }
    public String getStatus() { return status; }
    public String getPosterUrl() { return posterUrl; }
    public OffsetDateTime getSyncedAt() { return syncedAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setAnilistId(Integer v) { this.anilistId = v; }
    public void setAnissiaNo(Integer v) { this.anissiaNo = v; }
    public void setTmdbId(Integer v) { this.tmdbId = v; }
    public void setLaftelId(String v) { this.laftelId = v; }
    public void setTitleKo(String v) { this.titleKo = v; }
    public void setTitleRomaji(String v) { this.titleRomaji = v; }
    public void setTitleNative(String v) { this.titleNative = v; }
    public void setTitleEn(String v) { this.titleEn = v; }
    public void setSeason(String v) { this.season = v; }
    public void setSeasonYear(Short v) { this.seasonYear = v; }
    public void setStatus(String v) { this.status = v; }
    public void setPosterUrl(String v) { this.posterUrl = v; }
    public void setSyncedAt(OffsetDateTime v) { this.syncedAt = v; }
}
