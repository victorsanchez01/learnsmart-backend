package com.learnsmart.planning.controller;

import com.learnsmart.planning.dto.PlanDtos.*;
import com.learnsmart.planning.model.PlanActivity;
import com.learnsmart.planning.model.PlanModule;
import com.learnsmart.planning.service.PlanActivityService;
import com.learnsmart.planning.service.PlanModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanModuleController {

    private final PlanModuleService moduleService;
    private final PlanActivityService activityService;

    @GetMapping("/{planId}/modules")
    public ResponseEntity<List<ModuleResponse>> getPlanModules(@PathVariable UUID planId) {
        List<PlanModule> modules = moduleService.getModulesByPlan(planId);
        return ResponseEntity.ok(modules.stream().map(this::toModuleResponse).collect(Collectors.toList()));
    }

    @PatchMapping("/{planId}/modules/{moduleId}")
    public ResponseEntity<ModuleResponse> updateModule(
            @PathVariable UUID planId,
            @PathVariable UUID moduleId,
            @RequestBody UpdateModuleRequest request) {
        PlanModule updated = moduleService.updateModuleStatus(planId, moduleId, request.getStatus());
        return ResponseEntity.ok(toModuleResponse(updated));
    }

    @GetMapping("/{planId}/activities")
    public ResponseEntity<List<ActivityResponse>> getPlanActivities(@PathVariable UUID planId,
            @RequestParam(required = false) UUID moduleId) {
        List<PlanActivity> activities;
        if (moduleId != null) {
            activities = activityService.getActivitiesByModule(moduleId);
        } else {
            // Flatten activities from all modules of the plan
            activities = moduleService.getModulesByPlan(planId).stream()
                    .flatMap(m -> m.getActivities().stream())
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(activities.stream().map(this::toActivityResponse).collect(Collectors.toList()));
    }

    @PatchMapping("/{planId}/activities/{activityId}")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable UUID planId,
            @PathVariable UUID activityId,
            @RequestBody UpdateActivityRequest request) {
        PlanActivity updated = activityService.updateActivityStatus(planId, activityId, request.getStatus(),
                request.getOverrideEstimatedMinutes());
        return ResponseEntity.ok(toActivityResponse(updated));
    }

    private ModuleResponse toModuleResponse(PlanModule module) {
        ModuleResponse res = new ModuleResponse();
        res.setId(module.getId());
        res.setPlanId(module.getPlan().getId());
        res.setPosition(module.getPosition());
        res.setTitle(module.getTitle());
        res.setDescription(module.getDescription());
        res.setStatus(module.getStatus());
        res.setTargetSkills(module.getTargetSkills());
        return res;
    }

    private ActivityResponse toActivityResponse(PlanActivity activity) {
        ActivityResponse res = new ActivityResponse();
        res.setId(activity.getId());
        res.setModuleId(activity.getModule().getId());
        res.setPosition(activity.getPosition());
        res.setActivityType(activity.getActivityType());
        res.setStatus(activity.getStatus());
        res.setContentRef(activity.getContentRef());
        res.setEstimatedMinutes(activity.getEstimatedMinutes());
        return res;
    }
}
