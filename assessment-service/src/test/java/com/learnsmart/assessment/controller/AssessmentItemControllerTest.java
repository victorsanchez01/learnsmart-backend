package com.learnsmart.assessment.controller;

import com.learnsmart.assessment.dto.AssessmentDtos;
import com.learnsmart.assessment.model.AssessmentItem;
import com.learnsmart.assessment.service.AssessmentItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentItemControllerTest {

    @Mock
    private AssessmentItemService assessmentItemService;

    @InjectMocks
    private AssessmentItemController controller;

    // -------------------------------------------------------------------------
    // create — with difficulty, options, skills all non-null
    // -------------------------------------------------------------------------

    @Test
    void testCreate_WithAllFields() {
        AssessmentDtos.AssessmentItemInput input = new AssessmentDtos.AssessmentItemInput();
        input.setDomainId(UUID.randomUUID());
        input.setStem("Question");
        input.setDifficulty(0.5);

        AssessmentDtos.OptionInput optionInput = new AssessmentDtos.OptionInput();
        optionInput.setText("Option A");
        optionInput.setCorrect(true);
        optionInput.setFeedback("Correct!");
        input.setOptions(List.of(optionInput));

        AssessmentDtos.SkillInput skillInput = new AssessmentDtos.SkillInput();
        skillInput.setSkillId(UUID.randomUUID());
        skillInput.setWeight(1.0);
        input.setSkills(List.of(skillInput));

        when(assessmentItemService.create(any(AssessmentItem.class))).thenAnswer(i -> {
            AssessmentItem res = i.getArgument(0);
            res.setId(UUID.randomUUID());
            return res;
        });

        ResponseEntity<AssessmentItem> response = controller.create(input);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
        // Difficulty must have been mapped
        assertEquals(new BigDecimal("0.5"), response.getBody().getDifficulty());
        verify(assessmentItemService).create(any(AssessmentItem.class));
    }

    // -------------------------------------------------------------------------
    // create — difficulty null branch (no setDifficulty called)
    // -------------------------------------------------------------------------

    @Test
    void testCreate_NullDifficulty_DoesNotSetDifficulty() {
        AssessmentDtos.AssessmentItemInput input = new AssessmentDtos.AssessmentItemInput();
        input.setDomainId(UUID.randomUUID());
        input.setStem("Question without difficulty");
        input.setDifficulty(null); // null branch
        input.setOptions(null);
        input.setSkills(null);

        when(assessmentItemService.create(any(AssessmentItem.class))).thenAnswer(i -> {
            AssessmentItem res = i.getArgument(0);
            res.setId(UUID.randomUUID());
            return res;
        });

        ResponseEntity<AssessmentItem> response = controller.create(input);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNull(response.getBody().getDifficulty(), "Difficulty should remain null when input difficulty is null");
    }

    // -------------------------------------------------------------------------
    // findAll
    // -------------------------------------------------------------------------

    @Test
    void testFindAll() {
        when(assessmentItemService.findAll()).thenReturn(Collections.emptyList());

        ResponseEntity<List<AssessmentItem>> response = controller.findAll();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void testFindById_Found() {
        UUID id = UUID.randomUUID();
        AssessmentItem item = new AssessmentItem();
        item.setId(id);
        when(assessmentItemService.findById(id)).thenReturn(Optional.of(item));

        ResponseEntity<AssessmentItem> response = controller.findById(id);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void testFindById_NotFound() {
        UUID id = UUID.randomUUID();
        when(assessmentItemService.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<AssessmentItem> response = controller.findById(id);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
