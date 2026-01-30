package com.learnsmart.planning.service;

import com.learnsmart.planning.model.PlanActivity;
import com.learnsmart.planning.repository.PlanActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanActivityServiceImplementation implements PlanActivityService {

    private final PlanActivityRepository activityRepository;

    @Override
    public List<PlanActivity> getActivitiesByModule(UUID moduleId) {
        return activityRepository.findByModuleIdOrderByPositionAsc(moduleId);
    }

    @Override
    @Transactional
    public PlanActivity updateActivityStatus(UUID planId, UUID activityId, String status, Integer overrideMinutes) {
        PlanActivity activity = findById(activityId);
        // Verify plan consistency (optional but good practice)
        if (!activity.getModule().getPlan().getId().equals(planId)) {
            throw new RuntimeException("Activity does not belong to plan");
        }

        activity.setStatus(status);
        if (overrideMinutes != null) {
            activity.setOverrideEstimatedMinutes(overrideMinutes);
        }
        return activityRepository.save(activity);
    }

    @Override
    public PlanActivity findById(UUID activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found: " + activityId));
    }
}
