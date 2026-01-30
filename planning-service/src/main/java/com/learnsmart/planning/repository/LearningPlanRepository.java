package com.learnsmart.planning.repository;

import com.learnsmart.planning.model.LearningPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface LearningPlanRepository extends JpaRepository<LearningPlan, UUID> {
    Page<LearningPlan> findByStatus(String status, Pageable pageable);

    Page<LearningPlan> findByUserId(String userId, Pageable pageable); // Assuming security context filtering

    Page<LearningPlan> findByUserIdAndStatus(String userId, String status, Pageable pageable);
}
