package com.miroyiro.backend.domain.news.repository;

import java.util.Optional;

import com.miroyiro.backend.domain.news.entity.NewsFeed;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsFeedRepository extends JpaRepository<NewsFeed, Long> {

    Optional<NewsFeed> findByProcessedItemId(Long processedItemId);

    Optional<NewsFeed> findBySourceUrl(String sourceUrl);
}
