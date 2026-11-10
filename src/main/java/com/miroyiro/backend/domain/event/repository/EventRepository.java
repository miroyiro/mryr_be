package com.miroyiro.backend.domain.event.repository;

import java.util.Optional;

import com.miroyiro.backend.domain.event.entity.Event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByProcessedItemId(Long processedItemId);

    Optional<Event> findBySourceAndSourceId(String source, String sourceId);
}
