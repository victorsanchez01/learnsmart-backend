package com.learnsmart.assessment.controller;

import com.learnsmart.assessment.model.AssessmentItem;
import com.learnsmart.assessment.service.AssessmentItemService;
import com.learnsmart.assessment.dto.AssessmentDtos;
import com.learnsmart.assessment.model.AssessmentItemOption;
import com.learnsmart.assessment.model.AssessmentItemSkill;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@RestController
@RequestMapping("/assessment-items")
@RequiredArgsConstructor
public class AssessmentItemController {

    private final AssessmentItemService assessmentItemService;

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssessmentItem> create(@RequestBody AssessmentDtos.AssessmentItemInput input) {
        AssessmentItem item = new AssessmentItem();
        item.setDomainId(input.getDomainId());
        item.setType(input.getType());
        item.setStem(input.getStem());
        if (input.getDifficulty() != null) {
            item.setDifficulty(BigDecimal.valueOf(input.getDifficulty()));
        }
        item.setMetadata(input.getMetadata());

        if (input.getOptions() != null) {
            item.setOptions(input.getOptions().stream().map(o -> {
                AssessmentItemOption opt = new AssessmentItemOption();
                opt.setStatement(o.getText());
                opt.setIsCorrect(o.isCorrect());
                opt.setFeedbackTemplate(o.getFeedback());
                return opt;
            }).toList());
        }

        if (input.getSkills() != null) {
            item.setSkills(input.getSkills().stream().map(s -> {
                AssessmentItemSkill skill = new AssessmentItemSkill();
                // Initialize ID with null item ID, but valid skill ID
                // Hibernate @MapsId will fill assessmentItemId from setAssessmentItem in
                // Service
                skill.setId(new AssessmentItemSkill.AssessmentItemSkillId(null, s.getSkillId()));
                skill.setWeight(BigDecimal.valueOf(s.getWeight()));
                return skill;
            }).toList());
        }

        return ResponseEntity.status(201).body(assessmentItemService.create(item));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssessmentItem> findById(@PathVariable UUID id) {
        return assessmentItemService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<AssessmentItem>> findAll() {
        return ResponseEntity.ok(assessmentItemService.findAll());
    }
}
