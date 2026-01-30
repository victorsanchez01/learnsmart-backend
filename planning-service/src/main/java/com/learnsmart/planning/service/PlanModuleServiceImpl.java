package com.learnsmart.planning.service;

import com.learnsmart.planning.model.PlanModule;
import com.learnsmart.planning.repository.PlanModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanModuleServiceImpl implements PlanModuleService {

    private final PlanModuleRepository moduleRepository;

    @Override
    public List<PlanModule> getModulesByPlan(UUID planId) {
        return moduleRepository.findByPlanIdOrderByPositionAsc(planId);
    }

    @Override
    @Transactional
    public PlanModule updateModuleStatus(UUID planId, UUID moduleId, String status) {
        PlanModule module = findById(moduleId);
        if (!module.getPlan().getId().equals(planId)) {
            throw new RuntimeException("Module does not belong to plan");
        }
        module.setStatus(status);
        return moduleRepository.save(module);
    }

    @Override
    public PlanModule findById(UUID moduleId) {
        return moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found: " + moduleId));
    }
}
