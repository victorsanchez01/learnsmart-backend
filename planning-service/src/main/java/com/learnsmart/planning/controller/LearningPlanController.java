package com.learnsmart.planning.controller;

import com.learnsmart.planning.model.LearningPlan;
import com.learnsmart.planning.service.LearningPlanService;
import com.learnsmart.planning.dto.PlanDtos;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

import com.learnsmart.planning.dto.ExternalDtos;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class LearningPlanController {

    private final LearningPlanService planService;
    private final com.learnsmart.planning.service.ReplanTriggerService triggerService;

    @PostMapping("/diagnostics")
    public ResponseEntity<ExternalDtos.GenerateDiagnosticTestResponse> generateDiagnosticTest(
            @RequestBody ExternalDtos.GenerateDiagnosticTestRequest request) {
        List<Map<String, Object>> questions = planService.generateDiagnosticTest(
                request.getDomainId(), request.getLevel(), request.getNQuestions());

        return new ResponseEntity<>(new ExternalDtos.GenerateDiagnosticTestResponse(questions), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<LearningPlan> createPlan(@RequestBody LearningPlan plan) {
        return new ResponseEntity<>(planService.createPlan(plan), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LearningPlan> getPlan(@PathVariable UUID id) {
        return new ResponseEntity<>(planService.findById(id), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<Page<LearningPlan>> getPlans(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (userId != null) {
            return new ResponseEntity<>(planService.findByUser(userId, status, page, size), HttpStatus.OK);
        }
        return new ResponseEntity<>(planService.findAll(status, page, size), HttpStatus.OK);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LearningPlan> updatePlan(@PathVariable UUID id, @RequestBody LearningPlan planUpdates) {
        return new ResponseEntity<>(planService.updatePlan(id, planUpdates), HttpStatus.OK);
    }

    @PostMapping("/{id}/replan")
    public ResponseEntity<PlanDtos.PlanSummaryResponse> replan(@PathVariable UUID id, @RequestParam String reason,
            @RequestBody(required = false) String constraints) {
        LearningPlan updated = planService.replan(id, reason, constraints);
        return new ResponseEntity<>(toSummary(updated), HttpStatus.OK);
    }

    private PlanDtos.PlanSummaryResponse toSummary(LearningPlan plan) {
        PlanDtos.PlanSummaryResponse dto = new PlanDtos.PlanSummaryResponse();
        dto.setId(plan.getId());
        dto.setUserId(plan.getUserId());
        dto.setGoalId(plan.getGoalId());
        dto.setStatus(plan.getStatus());
        dto.setGeneratedBy(plan.getGeneratedBy());
        dto.setStartDate(plan.getStartDate());
        dto.setEndDate(plan.getEndDate());
        dto.setHoursPerWeek(plan.getHoursPerWeek());
        dto.setRawPlanAi(plan.getRawPlanAi());
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setUpdatedAt(plan.getUpdatedAt());
        if (plan.getModules() != null) {
            dto.setModules(plan.getModules().stream().map(m -> {
                PlanDtos.PlanSummaryResponse.ModuleSummary ms = new PlanDtos.PlanSummaryResponse.ModuleSummary();
                ms.setId(m.getId());
                ms.setPosition(m.getPosition());
                ms.setTitle(m.getTitle());
                ms.setStatus(m.getStatus());
                ms.setEstimatedHours(m.getEstimatedHours());
                ms.setTargetSkills(m.getTargetSkills());
                return ms;
            }).collect(Collectors.toList()));
        }
        return dto;
    }

    @GetMapping("/{id}/replan-triggers")
    public ResponseEntity<List<com.learnsmart.planning.dto.PlanDtos.ReplanTriggerResponse>> getReplanTriggers(
            @PathVariable UUID id) {
        return ResponseEntity.ok(triggerService.findPendingTriggers(id).stream()
                .map(this::mapToTriggerResponse)
                .collect(java.util.stream.Collectors.toList()));
    }

    private com.learnsmart.planning.dto.PlanDtos.ReplanTriggerResponse mapToTriggerResponse(
            com.learnsmart.planning.model.ReplanTrigger trigger) {
        com.learnsmart.planning.dto.PlanDtos.ReplanTriggerResponse res = new com.learnsmart.planning.dto.PlanDtos.ReplanTriggerResponse();
        res.setId(trigger.getId());
        res.setPlanId(trigger.getPlan().getId());
        res.setTriggerType(trigger.getTriggerType());
        res.setTriggerReason(trigger.getTriggerReason());
        res.setSeverity(trigger.getSeverity());
        res.setDetectedAt(trigger.getDetectedAt());
        res.setStatus(trigger.getStatus());
        res.setMetadata(trigger.getMetadata());
        return res;
    }

    @GetMapping("/certificates")
    public ResponseEntity<List<com.learnsmart.planning.model.Certificate>> getCertificates(
            @RequestParam String userId) {
        return new ResponseEntity<>(planService.getCertificates(UUID.fromString(userId)), HttpStatus.OK);
    }
}
