package com.learnsmart.profile.service;

import com.learnsmart.profile.client.Clients.*;
import com.learnsmart.profile.dto.ProgressDtos.*;
import com.learnsmart.profile.model.UserGoal;
import com.learnsmart.profile.model.UserProfile;
import com.learnsmart.profile.repository.UserGoalRepository;
import com.learnsmart.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserProfileRepository profileRepository;
    private final UserGoalRepository goalRepository;
    private final PlanningClient planningClient;
    private final AssessmentClient assessmentClient;
    private final TrackingClient trackingClient;

    public UserProgressResponse getConsolidatedProgress(String authUserId) {
        UserProfile profile = profileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("Profile not found for authUserId: " + authUserId));

        UserProgressResponse res = new UserProgressResponse();
        res.setProfile(new ProfileInfo(profile.getUserId().toString(), profile.getDisplayName()));

        // 1. Goals Progress
        List<UserGoal> goals = goalRepository.findByUserId(profile.getUserId());
        if (goals != null) {
            res.setGoals(goals.stream()
                    .map(g -> new GoalProgress(g.getId(), g.getTitle(), 0.0))
                    .collect(Collectors.toList()));
        }

        // 2. Planning - Current Plan
        try {
            // Using authUserId for planning-service as it usually identifies the user
            // globally or we use external ID
            Map<String, Object> plans = planningClient.getPlans(profile.getUserId().toString());
            List<Map<String, Object>> planList = (List<Map<String, Object>>) plans.get("content");
            if (planList != null && !planList.isEmpty()) {
                Map<String, Object> latestPlan = planList.get(0);
                UUID planId = UUID.fromString(latestPlan.get("id").toString());

                PlanProgress pp = new PlanProgress();
                pp.setPlanId(planId);
                pp.setGoalId(latestPlan.get("goalId") != null ? latestPlan.get("goalId").toString() : null);
                pp.setStatus(latestPlan.get("status").toString());

                // Fetch modules to calculate progress
                List<Map<String, Object>> modules = planningClient.getModules(planId);
                if (modules != null) {
                    pp.setTotalModules(modules.size());
                    long completed = modules.stream()
                            .filter(m -> "COMPLETED".equalsIgnoreCase(String.valueOf(m.get("status"))))
                            .count();
                    pp.setCompletedModules((int) completed);
                    pp.setOverallPercentage(modules.isEmpty() ? 0.0 : (completed * 100.0 / modules.size()));
                }
                res.setCurrentPlan(pp);
            }
        } catch (Exception e) {
            System.err.println("Error fetching planning data: " + e.getMessage());
        }

        // 3. Assessment - Skills in Progress
        try {
            List<Map<String, Object>> mastery = assessmentClient.getSkillMastery(profile.getUserId());
            if (mastery != null) {
                res.setSkillsInProgress(mastery.stream()
                        .map(m -> new SkillMasteryShort(
                                UUID.fromString(m.get("skillId").toString()),
                                String.valueOf(m.get("skillName")),
                                ((Number) m.get("mastery")).doubleValue()))
                        .limit(5)
                        .collect(Collectors.toList()));
            }
        } catch (Exception e) {
            System.err.println("Error fetching assessment data: " + e.getMessage());
        }

        // 4. Tracking - Activity Summary
        try {
            Map<String, Object> stats = trackingClient.getUserStats(profile.getUserId());
            if (stats != null) {
                ActivitySummary as = new ActivitySummary();
                as.setTotalHours(((Number) stats.getOrDefault("totalHours", 0.0)).doubleValue());
                as.setCurrentStreak(((Number) stats.getOrDefault("currentStreak", 0)).intValue());
                res.setActivity(as);
            }
        } catch (Exception e) {
            System.err.println("Error fetching tracking data: " + e.getMessage());
        }

        return res;
    }
}
