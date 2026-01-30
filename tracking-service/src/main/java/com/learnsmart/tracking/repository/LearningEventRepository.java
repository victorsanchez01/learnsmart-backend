package com.learnsmart.tracking.repository;

import com.learnsmart.tracking.model.LearningEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface LearningEventRepository extends JpaRepository<LearningEvent, UUID> {

    @Query("SELECT e FROM LearningEvent e WHERE " +
            "(:userId IS NULL OR e.userId = :userId) AND " +
            "(:eventType IS NULL OR e.eventType = :eventType) AND " +
            "(:entityType IS NULL OR e.entityType = :entityType) AND " +
            "(:entityId IS NULL OR e.entityId = :entityId) AND " +
            "(cast(:from as timestamp) IS NULL OR e.occurredAt >= :from) AND " +
            "(cast(:to as timestamp) IS NULL OR e.occurredAt <= :to) AND " +
            "e.deletedAt IS NULL")
    Page<LearningEvent> findEvents(
            @Param("userId") UUID userId,
            @Param("eventType") String eventType,
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);
}
