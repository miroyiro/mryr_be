package com.miroyiro.backend.domain.event.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * miroyiro_main.event (ERD v1.2).
 * 멱등키: processed_item_id (UNIQUE) — scrap 이 승인 요청을 재시도해도 행 1개.
 * 자연키: (source, source_id) UNIQUE — 같은 원본 글이 다시 들어오면 갱신.
 * artist_id / anime_id 는 FK 지만 지금은 id 값으로만 들고 있음 (연관관계 매핑은 /api/events 만들 때).
 */
@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** jpop | anime */
    @Column(name = "category", length = 10, nullable = false)
    private String category;

    @Column(name = "artist_id")
    private Long artistId;

    @Column(name = "anime_id")
    private Long animeId;

    /** concert | broadcast | streaming | popup | collabo_cafe | fair | exhibition | release | etc */
    @Column(name = "type", length = 20, nullable = false)
    private String type;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "venue", length = 200)
    private String venue;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "lat", precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(name = "lng", precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "is_all_day", nullable = false)
    private boolean allDay;

    /** upcoming | ongoing | ended | canceled — EventStatusSyncJob 이 매일 갱신 */
    @Column(name = "status", length = 10, nullable = false)
    private String status = "upcoming";

    @Column(name = "is_international", nullable = false)
    private boolean international;

    @Column(name = "reservation_required")
    private Boolean reservationRequired;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "price_info", length = 255)
    private String priceInfo;

    @Column(name = "ticket_url", length = 500)
    private String ticketUrl;

    @Column(name = "source", length = 20, nullable = false)
    private String source;

    @Column(name = "source_id", length = 100)
    private String sourceId;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "processed_item_id")
    private Long processedItemId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Event() {
    }

    public Event(String source, String sourceId) {
        this.source = source;
        this.sourceId = sourceId;
    }

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public Long getArtistId() { return artistId; }
    public Long getAnimeId() { return animeId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getVenue() { return venue; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public BigDecimal getLat() { return lat; }
    public BigDecimal getLng() { return lng; }
    public OffsetDateTime getStartAt() { return startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public boolean isAllDay() { return allDay; }
    public String getStatus() { return status; }
    public boolean isInternational() { return international; }
    public Boolean getReservationRequired() { return reservationRequired; }
    public String getPosterUrl() { return posterUrl; }
    public String getDescription() { return description; }
    public String getPriceInfo() { return priceInfo; }
    public String getTicketUrl() { return ticketUrl; }
    public String getSource() { return source; }
    public String getSourceId() { return sourceId; }
    public String getSourceUrl() { return sourceUrl; }
    public Long getProcessedItemId() { return processedItemId; }

    public void setCategory(String v) { this.category = v; }
    public void setArtistId(Long v) { this.artistId = v; }
    public void setAnimeId(Long v) { this.animeId = v; }
    public void setType(String v) { this.type = v; }
    public void setTitle(String v) { this.title = v; }
    public void setVenue(String v) { this.venue = v; }
    public void setAddress(String v) { this.address = v; }
    public void setCity(String v) { this.city = v; }
    public void setLat(BigDecimal v) { this.lat = v; }
    public void setLng(BigDecimal v) { this.lng = v; }
    public void setStartAt(OffsetDateTime v) { this.startAt = v; }
    public void setEndAt(OffsetDateTime v) { this.endAt = v; }
    public void setAllDay(boolean v) { this.allDay = v; }
    public void setStatus(String v) { this.status = v; }
    public void setInternational(boolean v) { this.international = v; }
    public void setReservationRequired(Boolean v) { this.reservationRequired = v; }
    public void setPosterUrl(String v) { this.posterUrl = v; }
    public void setDescription(String v) { this.description = v; }
    public void setPriceInfo(String v) { this.priceInfo = v; }
    public void setTicketUrl(String v) { this.ticketUrl = v; }
    public void setSourceUrl(String v) { this.sourceUrl = v; }
    public void setProcessedItemId(Long v) { this.processedItemId = v; }
}
