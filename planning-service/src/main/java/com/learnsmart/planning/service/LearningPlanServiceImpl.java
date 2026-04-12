package com.learnsmart.planning.service;

import com.learnsmart.planning.model.*;
import com.learnsmart.planning.repository.*;
import com.learnsmart.planning.client.SkillPrerequisiteClient;
import com.learnsmart.planning.dto.PrerequisiteDtos;
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
import java.util.Objects;
import com.learnsmart.planning.client.Clients;
import com.learnsmart.planning.dto.ExternalDtos;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
@RequiredArgsConstructor
public class LearningPlanServiceImpl implements LearningPlanService {

    private final LearningPlanRepository planRepository;
    private final PlanReplanHistoryRepository replanRepository;
    private final CertificateRepository certificateRepository;
    private final Clients.ProfileClient profileClient;
    private final Clients.ContentClient contentClient;
    private final Clients.AiClient aiClient;
    private final ReplanTriggerService triggerService;
    private final ObjectMapper objectMapper; // For JSON serialization

    // US-111: Prerequisite Validation
    private final SkillPrerequisiteClient skillPrerequisiteClient;
    private final PrerequisiteValidationService prerequisiteValidator;

    @Override
    @Transactional
    public LearningPlan createPlan(LearningPlan plan) {

        // Only generate AI plan if modules are empty
        if (plan.getModules() == null || plan.getModules().isEmpty()) {
            try {
                // 1. Fetch Profile
                ExternalDtos.UserProfile profile = profileClient.getProfile(plan.getUserId().toString());
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

                // Resolve domain name from domainId (if provided) instead of hardcoding
                String domainName = "general";
                if (plan.getDomainId() != null && !plan.getDomainId().isBlank()) {
                    try {
                        ExternalDtos.DomainDto domain = contentClient.getDomain(plan.getDomainId());
                        if (domain != null && domain.getName() != null && !domain.getName().isBlank()) {
                            domainName = domain.getName();
                        }
                    } catch (Exception domainEx) {
                        System.err.println("Could not resolve domain name for id: " + plan.getDomainId()
                                + " — " + domainEx.getMessage());
                    }
                }

                // Use plan name or goal title if available
                String planTitle = plan.getPlanName() != null && !plan.getPlanName().isBlank()
                        ? plan.getPlanName() : "Learning Plan for " + domainName;

                // Goals
                aiRequest.setGoals(List.of(Map.of(
                        "goalId", plan.getGoalId() != null ? plan.getGoalId() : "general-learning",
                        "title", planTitle,
                        "domain", domainName
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
                        module.setEstimatedHours(new BigDecimal("1.0")); // Default

                        List<PlanActivity> activities = new ArrayList<>();
                        int actIdx = 1;
                        for (ExternalDtos.ActivityDraft actDraft : modDraft.getActivities()) {
                            PlanActivity activity = new PlanActivity();
                            activity.setModule(module);
                            activity.setPosition(actIdx++);
                            activity.setActivityType(actDraft.getType());
                            String ref = actDraft.getContentRef();
                            if (ref == null || ref.isBlank()) {
                                ref = "manual:" + UUID.randomUUID();
                            }
                            activity.setContentRef(ref);
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
            }
        } else {
            // If modules are provided manually, verify links
            int manualIdx = 1;
            for (PlanModule m : plan.getModules()) {
                m.setPlan(plan);
                if (m.getPosition() == null) {
                    m.setPosition(manualIdx++);
                }
                if (m.getActivities() != null) {
                    for (PlanActivity a : m.getActivities()) {
                        a.setModule(m);
                    }
                }
            }
        }

        // US-111: Prerequisite Validation
        // Note: Currently disabled as targetSkills are not populated by AI service
        // TODO: Enable once AI service populates targetSkills in module drafts
        if (plan.getModules() != null && !plan.getModules().isEmpty()) {
            try {
                // Extract skill IDs from plan
                List<UUID> skillIds = plan.getModules().stream()
                        .filter(m -> m.getTargetSkills() != null && !m.getTargetSkills().isEmpty())
                        .flatMap(m -> m.getTargetSkills().stream())
                        .map(skillRef -> {
                            try {
                                return UUID.fromString(skillRef);
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                if (!skillIds.isEmpty()) {
                    // Fetch skill graph
                    Map<UUID, List<UUID>> skillGraph = skillPrerequisiteClient.getSkillGraph(skillIds);

                    // Validate prerequisites
                    List<PrerequisiteDtos.PrerequisiteViolation> violations = prerequisiteValidator.validatePlan(plan,
                            skillGraph);

                    if (!violations.isEmpty()) {
                        System.out.println("US-111: Prerequisite violations detected: " + violations.size());

                        // Attempt automatic re-ordering
                        try {
                            plan = prerequisiteValidator.reorderForPrerequisites(plan, skillGraph);
                            System.out.println("US-111: Successfully reordered plan to satisfy prerequisites");
                        } catch (Exception reorderEx) {
                            System.err.println("US-111: Failed to reorder plan: " + reorderEx.getMessage());
                            // Continue with original plan but log warning
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("US-111: Error during prerequisite validation: " + e.getMessage());
                // Continue with plan generation even if validation fails
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

        try {
            // 1. Prepare Request
            // Manually build map to avoid circular references in replans/modules
            Map<String, Object> currentPlanMap = new java.util.HashMap<>();
            currentPlanMap.put("id", existing.getId());
            currentPlanMap.put("userId", existing.getUserId());
            currentPlanMap.put("goalId", existing.getGoalId());
            currentPlanMap.put("status", existing.getStatus());
            currentPlanMap.put("startDate", existing.getStartDate());
            currentPlanMap.put("endDate", existing.getEndDate());
            currentPlanMap.put("hoursPerWeek", existing.getHoursPerWeek());

            // Add modules without the circular plan reference
            if (existing.getModules() != null) {
                List<Map<String, Object>> modulesMap = existing.getModules().stream().map(m -> {
                    Map<String, Object> mData = new java.util.HashMap<>();
                    mData.put("id", m.getId());
                    mData.put("title", m.getTitle());
                    mData.put("position", m.getPosition());
                    mData.put("status", m.getStatus());
                    return mData;
                }).collect(Collectors.toList());
                currentPlanMap.put("modules", modulesMap);
            }

            ExternalDtos.ReplanRequest request = ExternalDtos.ReplanRequest.builder()
                    .userId(existing.getUserId())
                    .reason(reason != null ? reason : "")
                    .currentPlan(currentPlanMap)
                    .recentEvents(new ArrayList<>())
                    .updatedSkillState(new ArrayList<>())
                    .build();

            // 2. Call AI Service
            ExternalDtos.ReplanResponse response = aiClient.replan(request);

            // 3. Apply Changes
            if (response != null && response.getPlan() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> moduleDrafts = (List<Map<String, Object>>) response.getPlan().get("modules");

                if (moduleDrafts != null) {
                    // Clear existing modules and tasks to avoid uniqueness violations
                    existing.getModules().clear();
                    planRepository.saveAndFlush(existing);

                    List<PlanModule> newModules = new ArrayList<>();
                    int modIdx = 1;

                    for (Map<String, Object> modDraft : moduleDrafts) {
                        if (modDraft == null)
                            continue;
                        PlanModule module = new PlanModule();
                        module.setPlan(existing);
                        module.setPosition(modIdx++);
                        module.setTitle((String) modDraft.get("title"));
                        module.setDescription((String) modDraft.get("description"));
                        module.setEstimatedHours(new BigDecimal("1.0"));

                        List<PlanActivity> activities = new ArrayList<>();
                        int actIdx = 1;
                        Object activitiesRaw = modDraft.get("activities");
                        if (activitiesRaw instanceof List) {
                            for (Object actRaw : (List<?>) activitiesRaw) {
                                PlanActivity activity = new PlanActivity();
                                activity.setModule(module);
                                activity.setPosition(actIdx++);

                                if (actRaw instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> actDraft = (Map<String, Object>) actRaw;
                                    activity.setActivityType((String) actDraft.get("type"));
                                    String ref = (String) actDraft.get("contentRef");
                                    if (ref == null || ref.isBlank()) {
                                        ref = "sys:" + UUID.randomUUID().toString().substring(0, 8);
                                    }
                                    activity.setContentRef(ref);
                                } else {
                                    // LLM returned activity as a plain String (e.g. "lesson")
                                    activity.setActivityType(actRaw != null ? actRaw.toString() : "lesson");
                                    activity.setContentRef("sys:" + UUID.randomUUID().toString().substring(0, 8));
                                }
                                activity.setEstimatedMinutes(20);
                                activities.add(activity);
                            }
                        }
                        module.setActivities(activities);
                        newModules.add(module);
                    }

                    existing.getModules().clear();
                    existing.getModules().addAll(newModules);
                }
                existing.setRawPlanAi(objectMapper.writeValueAsString(response));
            }

            // 4. Log History
            PlanReplanHistory history = new PlanReplanHistory();
            history.setPlan(existing);
            history.setReason(reason);
            history.setRequestPayload(constraints != null ? constraints : "Replan triggered");
            history.setResponsePayload(response != null ? response.getChangeSummary() : "No AI response");

            // US-107: Link to trigger if this replan was triggered automatically
            // Check for pending HIGH severity triggers and mark as EXECUTED
            if (reason != null && reason.contains("trigger")) {
                triggerService.findPendingTriggers(id).stream()
                        .filter(t -> "HIGH".equals(t.getSeverity()))
                        .findFirst()
                        .ifPresent(trigger -> {
                            trigger.setStatus("EXECUTED");
                            trigger.setEvaluatedAt(java.time.OffsetDateTime.now());
                            history.setTriggerId(trigger.getId());
                        });
            }

            replanRepository.save(history);

            return planRepository.save(existing);

        } catch (Exception e) {
            throw new RuntimeException("Replan failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> generateDiagnosticTest(String domainId, String level, int nQuestions) {
        // Resolve domain UUID to human-readable name so the AI generates
        // domain-specific questions.
        String domainName = domainId;
        try {
            ExternalDtos.DomainDto domain = contentClient.getDomain(domainId);
            if (domain != null && domain.getName() != null && !domain.getName().isBlank()) {
                domainName = domain.getName();
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot resolve domain '" + domainId + "' from content-service: " + e.getMessage(), e);
        }

        ExternalDtos.GenerateDiagnosticTestRequest request = ExternalDtos.GenerateDiagnosticTestRequest.builder()
                .domainId(domainId)
                .domainName(domainName)
                .level(level)
                .nQuestions(nQuestions)
                .build();
        ExternalDtos.GenerateDiagnosticTestResponse response = aiClient.generateDiagnosticTest(request);
        return response.getQuestions();
    }

    @Override
    public List<Certificate> getCertificates(UUID userId) {
        return certificateRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void checkCompletion(UUID planId) {
        if (certificateRepository.existsByPlanId(planId))
            return;

        LearningPlan plan = findById(planId);
        boolean allModulesCompleted = plan.getModules().stream()
                .allMatch(m -> "completed".equalsIgnoreCase(m.getStatus()));

        if (allModulesCompleted) {
            Certificate cert = new Certificate();
            cert.setUserId(UUID.fromString(plan.getUserId()));
            cert.setPlanId(plan.getId());
            cert.setTitle(
                    "Certificate of Completion: " + (plan.getGoalId() != null ? plan.getGoalId() : "Learning Plan"));
            cert.setDescription("Awarded for successfully completing the learning plan.");
            certificateRepository.save(cert);

            plan.setStatus("completed");
            planRepository.save(plan);
        }
    }
}
