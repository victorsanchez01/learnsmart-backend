package com.learnsmart.tracking.service;

import com.learnsmart.tracking.model.LearningEvent;
import com.learnsmart.tracking.repository.LearningEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final LearningEventRepository repository;

    @Transactional
    public LearningEvent createEvent(LearningEvent event) {
        return repository.save(event);
    }

    public Page<LearningEvent> listEvents(UUID userId, String eventType, String entityType, UUID entityId,
            OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        return repository.findEvents(userId, eventType, entityType, entityId, from, to, pageable);
    }
}
