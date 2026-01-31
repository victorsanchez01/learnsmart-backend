package com.learnsmart.assessment.repository;

import com.learnsmart.assessment.model.AssessmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentItemRepository extends JpaRepository<AssessmentItem, UUID> {

    @Query(value = "SELECT * FROM assessment_items WHERE is_active = true ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<AssessmentItem> findRandomActiveItem();
}
