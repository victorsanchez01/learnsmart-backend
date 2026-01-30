package com.learnsmart.assessment.repository;

import com.learnsmart.assessment.model.AssessmentItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import java.util.Optional;

public interface AssessmentItemRepository extends JpaRepository<AssessmentItem, UUID> {

    // Filtering by domain, skills (if needed via join), origin
    @Query("SELECT i FROM AssessmentItem i WHERE (:domainId IS NULL OR i.domainId = :domainId) AND (:origin IS NULL OR i.origin = :origin) AND i.isActive = true")
    Page<AssessmentItem> findAllFiltered(@Param("domainId") UUID domainId, @Param("origin") String origin,
            Pageable pageable);

    // For selecting next item (random for now, adaptive later)
    @Query(value = "SELECT * FROM assessment_items WHERE is_active = true ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<AssessmentItem> findRandomActiveItem();
}
