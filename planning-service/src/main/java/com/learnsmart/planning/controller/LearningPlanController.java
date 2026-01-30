package com.learnsmart.planning.controller;

import com.learnsmart.planning.model.LearningPlan;
import com.learnsmart.planning.service.LearningPlanService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class LearningPlanController {

    private final LearningPlanService planService;

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

    @PutMapping("/{id}")
    public ResponseEntity<LearningPlan> updatePlan(@PathVariable UUID id, @RequestBody LearningPlan planUpdates) {
        return new ResponseEntity<>(planService.updatePlan(id, planUpdates), HttpStatus.OK);
    }

    @PostMapping("/{id}/replan")
    public ResponseEntity<LearningPlan> replan(@PathVariable UUID id, @RequestParam String reason,
            @RequestBody(required = false) String constraints) {
        return new ResponseEntity<>(planService.replan(id, reason, constraints), HttpStatus.OK);
    }
}
