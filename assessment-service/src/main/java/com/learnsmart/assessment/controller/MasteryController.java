package com.learnsmart.assessment.controller;

import com.learnsmart.assessment.model.UserSkillMastery;
import com.learnsmart.assessment.service.AssessmentSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class MasteryController {

    private final AssessmentSessionService sessionService;
    private final com.learnsmart.assessment.client.ContentClient contentClient;

    @GetMapping("/{userId}/skill-mastery")
    public ResponseEntity<List<com.learnsmart.assessment.dto.MasteryDtos.SkillMasteryEnriched>> getUserSkillMastery(
            @PathVariable UUID userId) {
        List<UserSkillMastery> masteryList = sessionService.getUserSkillMastery(userId);

        List<com.learnsmart.assessment.dto.MasteryDtos.SkillMasteryEnriched> enrichedList = masteryList.stream()
                .map(m -> {
                    com.learnsmart.assessment.dto.MasteryDtos.SkillMasteryEnriched enriched = new com.learnsmart.assessment.dto.MasteryDtos.SkillMasteryEnriched();
                    enriched.setSkillId(m.getId().getSkillId());
                    enriched.setMastery(m.getMastery() != null ? m.getMastery().doubleValue() : 0.0);
                    enriched.setAttempts(m.getAttempts());
                    enriched.setLastUpdate(m.getLastUpdate());

                    try {
                        com.learnsmart.assessment.dto.MasteryDtos.SkillInfo info = contentClient
                                .getSkill(m.getId().getSkillId());
                        if (info != null) {
                            enriched.setSkillName(info.getName());
                            if (info.getDomain() != null) {
                                enriched.setDomainName(info.getDomain().getName());
                            }
                        }
                    } catch (Exception e) {
                        // Fallback to ID if content-service fails or skill not found
                        enriched.setSkillName("Skill " + m.getId().getSkillId());
                    }

                    return enriched;
                })
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(enrichedList);
    }
}
