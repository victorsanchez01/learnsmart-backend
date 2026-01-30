package com.learnsmart.planning.repository;

import com.learnsmart.planning.model.PlanModule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface PlanModuleRepository extends JpaRepository<PlanModule, UUID> {
    List<PlanModule> findByPlanIdOrderByPositionAsc(UUID planId);
}
