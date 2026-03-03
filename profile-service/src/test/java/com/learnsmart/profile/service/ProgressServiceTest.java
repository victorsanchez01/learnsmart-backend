package com.learnsmart.profile.service;

import com.learnsmart.profile.client.Clients.AssessmentClient;
import com.learnsmart.profile.client.Clients.PlanningClient;
import com.learnsmart.profile.client.Clients.TrackingClient;
import com.learnsmart.profile.model.UserGoal;
import com.learnsmart.profile.model.UserProfile;
import com.learnsmart.profile.repository.UserGoalRepository;
import com.learnsmart.profile.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private UserProfileRepository profileRepository;
    @Mock
    private UserGoalRepository goalRepository;
    @Mock
    private PlanningClient planningClient;
    @Mock
    private AssessmentClient assessmentClient;
    @Mock
    private TrackingClient trackingClient;

    @InjectMocks
    private ProgressService progressService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserProfile profile(UUID userId, String authId) {
        UserProfile p = new UserProfile();
        p.setUserId(userId);
        p.setAuthUserId(authId);
        p.setDisplayName("Test User");
        return p;
    }

    // -------------------------------------------------------------------------
    // getConsolidatedProgress — happy path
    // -------------------------------------------------------------------------

    @Test
    void testGetConsolidatedProgress_AllDataAvailable() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        String authId = "auth-123";
        UserProfile p = profile(userId, authId);

        when(profileRepository.findByAuthUserId(authId)).thenReturn(Optional.of(p));
        when(goalRepository.findByUserId(userId)).thenReturn(List.of(
                UserGoal.builder().id(UUID.randomUUID()).title("Goal 1").build()));

        // Planning: one plan with two modules (one completed)
        Map<String, Object> planEntry = Map.of(
                "id", planId.toString(),
                "status", "in_progress",
                "goalId", UUID.randomUUID().toString());
        when(planningClient.getPlans(userId.toString()))
                .thenReturn(Map.of("content", List.of(planEntry)));
        when(planningClient.getModules(planId)).thenReturn(List.of(
                Map.of("status", "COMPLETED"),
                Map.of("status", "in_progress")));

        // Assessment: 2 skill mastery entries
        when(assessmentClient.getSkillMastery(userId)).thenReturn(List.of(
                Map.of("skillId", skillId.toString(), "skillName", "Java", "mastery", 0.75),
                Map.of("skillId", UUID.randomUUID().toString(), "skillName", "Spring", "mastery", 0.5)));

        // Tracking: activity summary
        when(trackingClient.getUserStats(userId)).thenReturn(
                Map.of("totalHours", 10.5, "currentStreak", 3));

        var result = progressService.getConsolidatedProgress(authId);

        assertNotNull(result);
        assertEquals(userId.toString(), result.getProfile().getUserId());
        assertEquals(1, result.getGoals().size());

        assertNotNull(result.getCurrentPlan());
        assertEquals(planId, result.getCurrentPlan().getPlanId());
        assertEquals(2, result.getCurrentPlan().getTotalModules());
        assertEquals(1, result.getCurrentPlan().getCompletedModules());
        assertEquals(50.0, result.getCurrentPlan().getOverallPercentage(), 0.001);

        assertEquals(2, result.getSkillsInProgress().size());
        assertEquals(0.75, result.getSkillsInProgress().get(0).getMastery(), 0.001);

        assertNotNull(result.getActivity());
        assertEquals(10.5, result.getActivity().getTotalHours(), 0.001);
        assertEquals(3, result.getActivity().getCurrentStreak());
    }

    @Test
    void testGetConsolidatedProgress_ProfileNotFound_Throws() {
        when(profileRepository.findByAuthUserId("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> progressService.getConsolidatedProgress("unknown"));
    }

    // -------------------------------------------------------------------------
    // External client failures are swallowed gracefully (fail-soft per design)
    // -------------------------------------------------------------------------

    @Test
    void testGetConsolidatedProgress_PlanningClientFails_ReturnsNullPlan() {
        UUID userId = UUID.randomUUID();
        String authId = "auth-fail-planning";
        UserProfile p = profile(userId, authId);

        when(profileRepository.findByAuthUserId(authId)).thenReturn(Optional.of(p));
        when(goalRepository.findByUserId(userId)).thenReturn(List.of());
        when(planningClient.getPlans(any())).thenThrow(new RuntimeException("Planning service unavailable"));
        when(assessmentClient.getSkillMastery(userId)).thenReturn(null);
        when(trackingClient.getUserStats(userId)).thenReturn(null);

        var result = progressService.getConsolidatedProgress(authId);

        assertNotNull(result, "Response must still be returned even if planning fails");
        assertNull(result.getCurrentPlan(), "currentPlan must be null when planning service fails");
    }

    @Test
    void testGetConsolidatedProgress_AssessmentClientFails_SkillsEmpty() {
        UUID userId = UUID.randomUUID();
        String authId = "auth-fail-assessment";
        UserProfile p = profile(userId, authId);

        when(profileRepository.findByAuthUserId(authId)).thenReturn(Optional.of(p));
        when(goalRepository.findByUserId(userId)).thenReturn(List.of());
        when(planningClient.getPlans(any())).thenReturn(Map.of()); // no "content" key → empty plan
        when(assessmentClient.getSkillMastery(userId)).thenThrow(new RuntimeException("Assessment down"));
        when(trackingClient.getUserStats(userId)).thenReturn(null);

        var result = progressService.getConsolidatedProgress(authId);

        assertNull(result.getSkillsInProgress(),
                "Skills list must remain null when assessment service fails");
    }

    @Test
    void testGetConsolidatedProgress_NoGoals_ReturnsEmptyList() {
        UUID userId = UUID.randomUUID();
        String authId = "auth-no-goals";
        UserProfile p = profile(userId, authId);

        when(profileRepository.findByAuthUserId(authId)).thenReturn(Optional.of(p));
        when(goalRepository.findByUserId(userId)).thenReturn(List.of());
        when(planningClient.getPlans(any())).thenReturn(Map.of());
        when(assessmentClient.getSkillMastery(userId)).thenReturn(null);
        when(trackingClient.getUserStats(userId)).thenReturn(null);

        var result = progressService.getConsolidatedProgress(authId);

        assertNotNull(result.getGoals());
        assertTrue(result.getGoals().isEmpty());
    }

    @Test
    void testGetConsolidatedProgress_SkillsLimitedTo5() {
        UUID userId = UUID.randomUUID();
        String authId = "auth-many-skills";
        UserProfile p = profile(userId, authId);

        when(profileRepository.findByAuthUserId(authId)).thenReturn(Optional.of(p));
        when(goalRepository.findByUserId(userId)).thenReturn(List.of());
        when(planningClient.getPlans(any())).thenReturn(Map.of());

        // Return 8 skill entries — the service must cap at 5
        List<Map<String, Object>> manySkills = List.of(
                Map.of("skillId", UUID.randomUUID().toString(), "skillName", "S1", "mastery", 0.9),
                Map.of("skillId", UUID.randomUUID().toString(), "skillName", "S2", "mastery", 0.8),
                Map.of("skillId", UUID.randomUUID().toString(), "skillName", "S3", "mastery", 0.7),
                Map.of("skillId", UUID.randomUUID().toString(), "skillName", "S4", "mastery", 0.6),
                Map.of("skillId", UUID.randomUUID().toString(), "skillName", "S5", "mastery", 0.5),
                Map.of("skillId", UUID.randomUUID().toString(), "skillName", "S6", "mastery", 0.4),
                Map.of("skillId", UUID.randomUUID().toString(), "skillName", "S7", "mastery", 0.3),
                Map.of("skillId", UUID.randomUUID().toString(), "skillName", "S8", "mastery", 0.2));
        when(assessmentClient.getSkillMastery(userId)).thenReturn(manySkills);
        when(trackingClient.getUserStats(userId)).thenReturn(null);

        var result = progressService.getConsolidatedProgress(authId);

        assertEquals(5, result.getSkillsInProgress().size(),
                "Skills in progress must be capped at 5");
    }
}
