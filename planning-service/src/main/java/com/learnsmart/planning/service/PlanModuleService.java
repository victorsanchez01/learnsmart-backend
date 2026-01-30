package com.learnsmart.planning.service;

import com.learnsmart.planning.model.PlanModule;
import java.util.UUID;
import java.util.List;

public interface PlanModuleService {
    List<PlanModule> getModulesByPlan(UUID planId);

    PlanModule updateModuleStatus(UUID planId, UUID moduleId, String status);

    PlanModule findById(UUID moduleId);
}
