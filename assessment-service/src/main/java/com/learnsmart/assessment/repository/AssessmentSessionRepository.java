package com.learnsmart.assessment.repository;

import com.learnsmart.assessment.model.AssessmentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, UUID> {
    List<AssessmentSession> findByUserIdAndStatus(UUID userId, String status);

    List<AssessmentSession> findByUserId(UUID userId);
}
