package com.learnsmart.planning.repository;

import com.learnsmart.planning.model.PlanModule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface PlanModuleRepository extends JpaRepository<PlanModule, UUID> {
    List<PlanModule> findByPlanIdOrderByPositionAsc(UUID planId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM PlanModule m WHERE m.plan.id = :planId")
    void deleteByPlanId(@org.springframework.data.repository.query.Param("planId") UUID planId);
}
