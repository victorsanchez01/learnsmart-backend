package com.learnsmart.assessment.service;

import com.learnsmart.assessment.model.AssessmentItem;
import com.learnsmart.assessment.model.AssessmentItemOption;
import com.learnsmart.assessment.model.AssessmentItemSkill;
import com.learnsmart.assessment.repository.AssessmentItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentItemServiceImplTest {

    @Mock
    private AssessmentItemRepository assessmentItemRepository;

    @InjectMocks
    private AssessmentItemServiceImpl assessmentItemService;

    @Test
    void testCreate() {
        AssessmentItem item = new AssessmentItem();
        item.setId(UUID.randomUUID());
        AssessmentItemOption option = new AssessmentItemOption();
        item.setOptions(List.of(option));
        AssessmentItemSkill skill = new AssessmentItemSkill();
        item.setSkills(List.of(skill));

        when(assessmentItemRepository.save(any(AssessmentItem.class))).thenReturn(item);

        AssessmentItem result = assessmentItemService.create(item);
        assertNotNull(result);
        assertEquals(item.getId(), result.getId());
        // Verify bidirectional relationship links are established
        assertEquals(item, option.getAssessmentItem());
        assertEquals(item, skill.getAssessmentItem());
        verify(assessmentItemRepository, times(1)).save(item);
    }

    @Test
    void testCreate_WithNullCollections() {
        // Verifies null skills/options do not cause NullPointerException (defensive
        // null
        // check in impl)
        AssessmentItem item = new AssessmentItem();
        item.setId(UUID.randomUUID());
        item.setSkills(null);
        item.setOptions(null);

        when(assessmentItemRepository.save(any(AssessmentItem.class))).thenReturn(item);

        AssessmentItem result = assertDoesNotThrow(() -> assessmentItemService.create(item));
        assertNotNull(result);
        verify(assessmentItemRepository, times(1)).save(item);
    }

    @Test
    void testFindById_Found() {
        UUID id = UUID.randomUUID();
        AssessmentItem item = new AssessmentItem();
        item.setId(id);

        when(assessmentItemRepository.findById(id)).thenReturn(Optional.of(item));

        Optional<AssessmentItem> result = assessmentItemService.findById(id);
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void testFindById_NotFound() {
        UUID id = UUID.randomUUID();
        when(assessmentItemRepository.findById(id)).thenReturn(Optional.empty());

        Optional<AssessmentItem> result = assessmentItemService.findById(id);
        assertFalse(result.isPresent());
        verify(assessmentItemRepository).findById(id);
    }

    @Test
    void testFindAll_ReturnsItems() {
        AssessmentItem item = new AssessmentItem();
        item.setId(UUID.randomUUID());
        when(assessmentItemRepository.findAll()).thenReturn(List.of(item));

        List<AssessmentItem> result = assessmentItemService.findAll();
        assertEquals(1, result.size());
        assertEquals(item.getId(), result.get(0).getId());
        verify(assessmentItemRepository).findAll();
    }

    @Test
    void testFindAll_Empty() {
        when(assessmentItemRepository.findAll()).thenReturn(Collections.emptyList());

        List<AssessmentItem> result = assessmentItemService.findAll();
        assertTrue(result.isEmpty());
        verify(assessmentItemRepository).findAll();
    }
}
