package com.miroyiro.backend.domain.news.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** miroyiro_main.news_feed — 아웃링크만 (본문 저장 X). UNIQUE(source_url), UNIQUE(processed_item_id). */
@Entity
@Table(name = "news_feed")
public class NewsFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category", length = 10, nullable = false)
    private String category;

    @Column(name = "artist_id")
    private Long artistId;

    @Column(name = "anime_id")
    private Long animeId;

    /** aniplus | x | umj | manual */
    @Column(name = "source", length = 20, nullable = false)
    private String source;

    @Column(name = "source_url", length = 500, nullable = false)
    private String sourceUrl;

    @Column(name = "title", length = 300, nullable = false)
    private String title;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "processed_item_id")
    private Long processedItemId;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected NewsFeed() {
    }

    public NewsFeed(String source, String sourceUrl) {
        this.source = source;
        this.sourceUrl = sourceUrl;
    }

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public Long getArtistId() { return artistId; }
    public Long getAnimeId() { return animeId; }
    public String getSource() { return source; }
    public String getSourceUrl() { return sourceUrl; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public Long getProcessedItemId() { return processedItemId; }
    public int getLikeCount() { return likeCount; }

    public void setCategory(String v) { this.category = v; }
    public void setArtistId(Long v) { this.artistId = v; }
    public void setAnimeId(Long v) { this.animeId = v; }
    public void setTitle(String v) { this.title = v; }
    public void setSummary(String v) { this.summary = v; }
    public void setThumbnailUrl(String v) { this.thumbnailUrl = v; }
    public void setPublishedAt(OffsetDateTime v) { this.publishedAt = v; }
    public void setProcessedItemId(Long v) { this.processedItemId = v; }
}
