package com.learnsmart.planning.service;

import com.learnsmart.planning.model.LearningPlan;
import org.springframework.data.domain.Page;
import java.util.UUID;
import java.util.List;

public interface LearningPlanService {
    LearningPlan createPlan(LearningPlan plan);

    LearningPlan findById(UUID id);

    Page<LearningPlan> findAll(String status, int page, int size);

    Page<LearningPlan> findByUser(String userId, String status, int page, int size);

    LearningPlan updatePlan(UUID id, LearningPlan planUpdates);

    // Replan logic will be separate or here? Let's keep it here for now.
    LearningPlan replan(UUID id, String reason, String constraints);
}
