package com.learnsmart.planning.service;

import com.learnsmart.planning.model.PlanActivity;
import java.util.UUID;
import java.util.List;

public interface PlanActivityService {
    List<PlanActivity> getActivitiesByModule(UUID moduleId); // Usually accessed via Module -> Activities

    PlanActivity updateActivityStatus(UUID planId, UUID activityId, String status, Integer overrideMinutes);

    PlanActivity findById(UUID activityId);
}
