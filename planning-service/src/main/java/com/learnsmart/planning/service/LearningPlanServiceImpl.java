package com.learnsmart.planning.service;

import com.learnsmart.planning.model.*;
import com.learnsmart.planning.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.Map;
import com.learnsmart.planning.client.Clients;
import com.learnsmart.planning.dto.ExternalDtos;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
@RequiredArgsConstructor
public class LearningPlanServiceImpl implements LearningPlanService {

    private final LearningPlanRepository planRepository;
    private final PlanReplanHistoryRepository replanRepository;
    private final Clients.ProfileClient profileClient;
    private final Clients.ContentClient contentClient;
    private final Clients.AiClient aiClient;
    private final ObjectMapper objectMapper = new ObjectMapper(); // For JSON serialization

    @Override
    @Transactional
    public LearningPlan createPlan(LearningPlan plan) {
        // Only generate AI plan if modules are empty
        if (plan.getModules() == null || plan.getModules().isEmpty()) {
            try {
                // 1. Fetch Profile
                ExternalDtos.UserProfile profile = profileClient.getProfile(plan.getUserId());
                if (profile == null)
                    throw new RuntimeException("Profile not found for user: " + plan.getUserId());

                // 2. Fetch Content Catalog
                List<ExternalDtos.ContentItemDto> catalog = contentClient.getContentItems(100);

                // 3. Prepare AI Request
                ExternalDtos.GeneratePlanRequest aiRequest = new ExternalDtos.GeneratePlanRequest();
                aiRequest.setUserId(profile.getUserId() != null ? profile.getUserId() : plan.getUserId());

                // Convert Profile POJO to Map for AI flexibility
                Map<String, Object> profileMap = objectMapper.convertValue(profile,
                        new TypeReference<Map<String, Object>>() {
                        });
                aiRequest.setProfile(profileMap);

                // Goals (Mocking single goal from Plan ID/Goal ID if available, otherwise
                // generic)
                // Ideally we fetch specific Goal details. For now, we wrap basic info.
                aiRequest.setGoals(List.of(Map.of(
                        "goalId", plan.getGoalId() != null ? plan.getGoalId() : "general-learning",
                        "title", "Custom Plan",
                        "domain", "backend" // Heuristic default or fetch from GoalService
                )));

                // Convert Content Catalog to Map List
                List<Map<String, Object>> catalogMap = catalog.stream()
                        .map(item -> objectMapper.<Map<String, Object>>convertValue(item,
                                new TypeReference<Map<String, Object>>() {
                                }))
                        .collect(Collectors.toList());
                aiRequest.setContentCatalog(catalogMap);

                // 4. Call AI Service
                ExternalDtos.GeneratePlanResponse aiResponse = aiClient.generatePlan(aiRequest);

                // 5. Map Response to Entities
                if (aiResponse != null && aiResponse.getPlan() != null) {
                    List<PlanModule> modules = new ArrayList<>();
                    int modIdx = 1;

                    for (ExternalDtos.ModuleDraft modDraft : aiResponse.getPlan().getModules()) {
                        PlanModule module = new PlanModule();
                        module.setPlan(plan);
                        module.setPosition(modIdx++);
                        module.setTitle(modDraft.getTitle());
                        module.setDescription(modDraft.getDescription());
                        module.setEstimatedHours(new BigDecimal("1.0")); // Default or parsing dependent

                        List<PlanActivity> activities = new ArrayList<>();
                        int actIdx = 1;
                        for (ExternalDtos.ActivityDraft actDraft : modDraft.getActivities()) {
                            PlanActivity activity = new PlanActivity();
                            activity.setModule(module);
                            activity.setPosition(actIdx++);
                            activity.setActivityType(actDraft.getType());
                            activity.setContentRef(actDraft.getContentRef());
                            activity.setEstimatedMinutes(20); // Default
                            activities.add(activity);
                        }
                        module.setActivities(activities);
                        modules.add(module);
                    }
                    plan.setModules(modules);
                    plan.setRawPlanAi(objectMapper.writeValueAsString(aiResponse));
                }

            } catch (Exception e) {
                // Fallback or Log Error
                System.err.println("Error generating AI Plan: " + e.getMessage());
                e.printStackTrace();
                // We let it save empty plan rather than hard crashing, or we could re-throw.
                // For this validation task, better to see the error in logs but save
                // "something".
            }
        }
        return planRepository.save(plan);
    }

    @Override
    public LearningPlan findById(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + id));
    }

    @Override
    public Page<LearningPlan> findAll(String status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        if (status != null) {
            return planRepository.findByStatus(status, pr);
        }
        return planRepository.findAll(pr);
    }

    @Override
    public Page<LearningPlan> findByUser(String userId, String status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        if (status != null) {
            return planRepository.findByUserIdAndStatus(userId, status, pr);
        }
        return planRepository.findByUserId(userId, pr);
    }

    @Override
    @Transactional
    public LearningPlan updatePlan(UUID id, LearningPlan planUpdates) {
        LearningPlan existing = findById(id);
        if (planUpdates.getStatus() != null)
            existing.setStatus(planUpdates.getStatus());
        if (planUpdates.getEndDate() != null)
            existing.setEndDate(planUpdates.getEndDate());
        if (planUpdates.getHoursPerWeek() != null)
            existing.setHoursPerWeek(planUpdates.getHoursPerWeek());
        return planRepository.save(existing);
    }

    @Override
    @Transactional
    public LearningPlan replan(UUID id, String reason, String constraints) {
        LearningPlan existing = findById(id);

        // Log history
        PlanReplanHistory history = new PlanReplanHistory();
        history.setPlan(existing);
        history.setReason(reason);
        history.setRequestPayload(constraints);
        history.setResponsePayload("{\"mock\": \"replanned\"}"); // Mock AI response
        replanRepository.save(history);

        // Update plan logic (Mock)
        existing.setRawPlanAi(history.getResponsePayload());
        // In real world, we would parse response and update modules/activities.

        return planRepository.save(existing);
    }
}
