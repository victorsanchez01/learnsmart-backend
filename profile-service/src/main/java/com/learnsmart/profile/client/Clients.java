package com.learnsmart.profile.client;

import com.learnsmart.profile.dto.ProgressDtos.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.UUID;
import java.util.Map;

public interface Clients {

    @FeignClient(name = "planning-service")
    public interface PlanningClient {
        @GetMapping("/plans")
        Map<String, Object> getPlans(@RequestParam("userId") String userId);

        @GetMapping("/plans/{planId}/modules")
        List<Map<String, Object>> getModules(@PathVariable("planId") UUID planId);
    }

    @FeignClient(name = "assessment-service")
    public interface AssessmentClient {
        @GetMapping("/users/{userId}/skill-mastery")
        List<Map<String, Object>> getSkillMastery(@PathVariable("userId") UUID userId);
    }

    @FeignClient(name = "tracking-service")
    public interface TrackingClient {
        @GetMapping("/analytics/users/{userId}/stats")
        Map<String, Object> getUserStats(@PathVariable("userId") UUID userId);
    }
}
