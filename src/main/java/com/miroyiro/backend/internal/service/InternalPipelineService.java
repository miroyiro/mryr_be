package com.miroyiro.backend.internal.service;

import com.miroyiro.backend.domain.event.entity.Event;
import com.miroyiro.backend.domain.event.repository.EventRepository;
import com.miroyiro.backend.domain.news.entity.NewsFeed;
import com.miroyiro.backend.domain.news.repository.NewsFeedRepository;
import com.miroyiro.backend.global.error.BusinessException;
import com.miroyiro.backend.global.error.ErrorCode;
import com.miroyiro.backend.internal.dto.EventPublishRequest;
import com.miroyiro.backend.internal.dto.IdResponse;
import com.miroyiro.backend.internal.dto.NewsPublishRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 승인된 processed_item → main.event / main.news_feed.
 *
 * 멱등 규칙 (scrap 과 합의, backend_client.py 주석)
 *  1) processed_item_id 로 이미 있으면 → 그 id 그대로 (created=false). 재시도 안전
 *  2) 자연키((source, source_id) / source_url)로 있으면 → 내용 갱신 + processed_item_id 연결 (재수집·재승인)
 *  3) 없으면 INSERT
 */
@Service
public class InternalPipelineService {

    private final EventRepository events;
    private final NewsFeedRepository news;

    public InternalPipelineService(EventRepository events, NewsFeedRepository news) {
        this.events = events;
        this.news = news;
    }

    @Transactional
    public IdResponse publishEvent(EventPublishRequest r) {
        var existing = events.findByProcessedItemId(r.processedItemId());
        if (existing.isPresent()) {
            return new IdResponse(existing.get().getId(), false);
        }
        if ("manual".equals(r.source()) && (r.sourceUrl() == null || r.sourceUrl().isBlank())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "manual 은 source_url 필수 (ck_event_manual_url)");
        }
        if (r.endAt() != null && r.endAt().isBefore(r.startAt())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "end_at < start_at (ck_event_dates)");
        }
        Event e = r.sourceId() == null ? null : events.findBySourceAndSourceId(r.source(), r.sourceId()).orElse(null);
        boolean created = e == null;
        if (created) {
            e = new Event(r.source(), r.sourceId());
        }
        e.setCategory(r.category());
        e.setType(r.type());
        e.setTitle(r.title());
        e.setVenue(r.venue());
        e.setAddress(r.address());
        e.setCity(r.city());
        e.setStartAt(r.startAt());
        e.setEndAt(r.endAt());
        e.setAllDay(Boolean.TRUE.equals(r.isAllDay()));
        e.setInternational(Boolean.TRUE.equals(r.isInternational()));
        e.setReservationRequired(r.reservationRequired());
        e.setPosterUrl(r.posterUrl());
        e.setDescription(r.description());
        e.setPriceInfo(r.priceInfo());
        e.setTicketUrl(r.ticketUrl());
        e.setSourceUrl(r.sourceUrl());
        e.setArtistId(r.artistId());
        e.setAnimeId(r.animeId());
        e.setProcessedItemId(r.processedItemId());
        return new IdResponse(events.save(e).getId(), created);
    }

    @Transactional
    public IdResponse publishNews(NewsPublishRequest r) {
        var existing = news.findByProcessedItemId(r.processedItemId());
        if (existing.isPresent()) {
            return new IdResponse(existing.get().getId(), false);
        }
        NewsFeed n = news.findBySourceUrl(r.sourceUrl()).orElse(null);
        boolean created = n == null;
        if (created) {
            n = new NewsFeed(r.source(), r.sourceUrl());
        }
        n.setCategory(r.category());
        n.setTitle(r.title());
        n.setSummary(r.summary());
        n.setThumbnailUrl(r.thumbnailUrl());
        n.setPublishedAt(r.publishedAt());
        n.setArtistId(r.artistId());
        n.setAnimeId(r.animeId());
        n.setProcessedItemId(r.processedItemId());
        return new IdResponse(news.save(n).getId(), created);
    }
}
