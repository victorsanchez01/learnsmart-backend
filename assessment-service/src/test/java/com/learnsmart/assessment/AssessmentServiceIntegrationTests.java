package com.learnsmart.assessment;

import com.learnsmart.assessment.model.*;
import com.learnsmart.assessment.service.*;
import com.learnsmart.assessment.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AssessmentServiceIntegrationTests {

    @Autowired
    private AssessmentSessionService sessionService;
    @Autowired
    private AssessmentItemRepository itemRepository;
    @Autowired
    private UserSkillMasteryRepository masteryRepository;

    @Test
    @Transactional
    void fullAssessmentFlow() {
        // 1. Setup: Create Item
        AssessmentItem item = new AssessmentItem();
        item.setDomainId(UUID.randomUUID());
        item.setType("multiple_choice");
        item.setStem("What is 2+2?");
        item.setDifficulty(new BigDecimal("0.5"));

        AssessmentItemOption opt1 = new AssessmentItemOption();
        opt1.setAssessmentItem(item);
        opt1.setStatement("4");
        opt1.setIsCorrect(true);
        opt1.setLabel("A");

        AssessmentItemOption opt2 = new AssessmentItemOption();
        opt2.setAssessmentItem(item);
        opt2.setStatement("5");
        opt2.setIsCorrect(false);
        opt2.setLabel("B");

        item.setOptions(new ArrayList<>(List.of(opt1, opt2)));

        AssessmentItemSkill skill = new AssessmentItemSkill();
        UUID skillId = UUID.randomUUID();
        AssessmentItemSkill.AssessmentItemSkillId skillKey = new AssessmentItemSkill.AssessmentItemSkillId(null,
                skillId);
        skill.setId(skillKey);
        skill.setAssessmentItem(item); // Link back
        // Hibernate requires managing bi-directional for cascade in some cases, but
        // manual save is safer here if not fully cascading ID logic.
        // Actually, @MapsId handled it.

        item.setSkills(new ArrayList<>(List.of(skill)));

        item = itemRepository.save(item);
        assertNotNull(item.getId());

        // 2. Start Session
        AssessmentSession session = new AssessmentSession();
        session.setUserId(UUID.randomUUID());
        session.setType("exam");
        AssessmentSession created = sessionService.createSession(session);
        assertEquals("in_progress", created.getStatus());

        // 3. Get Next Item
        AssessmentItem next = sessionService.getNextItem(created.getId());
        assertEquals(item.getId(), next.getId());

        // 4. Submit Correct Response
        SubmitResponseRequest req = new SubmitResponseRequest();
        req.setAssessmentItemId(next.getId());
        req.setSelectedOptionId(opt1.getId()); // Correct option
        req.setResponseTimeMs(1000);

        UserItemResponseWithFeedback res = sessionService.submitResponse(created.getId(), req);
        assertTrue(res.getIsCorrect());
        assertEquals("Correct!", res.getFeedback());

        // 5. Verify Mastery Update
        List<UserSkillMastery> masteries = masteryRepository.findByIdUserId(session.getUserId());
        assertFalse(masteries.isEmpty());
        UserSkillMastery m = masteries.get(0);
        assertEquals(skillId, m.getId().getSkillId());
        // Default 0.3 + 0.1 = 0.4
        assertEquals(0, new BigDecimal("0.4").compareTo(m.getMastery()));
    }
}
