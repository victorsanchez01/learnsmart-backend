package com.learnsmart.planning.repository;

import com.learnsmart.planning.model.PlanReplanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface PlanReplanHistoryRepository extends JpaRepository<PlanReplanHistory, UUID> {
    List<PlanReplanHistory> findByPlanIdOrderByCreatedAtDesc(UUID planId);
}
