package com.learnsmart.planning.repository;

import com.learnsmart.planning.model.PlanActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface PlanActivityRepository extends JpaRepository<PlanActivity, UUID> {
    List<PlanActivity> findByModuleIdOrderByPositionAsc(UUID moduleId);
}
