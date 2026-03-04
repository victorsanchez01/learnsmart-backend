package com.learnsmart.content.controller;

import com.learnsmart.content.dto.ContentDtos;
import com.learnsmart.content.model.ContentItem;
import com.learnsmart.content.model.Domain;
import com.learnsmart.content.service.ContentItemService;
import com.learnsmart.content.service.DomainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentItemControllerTest {

    @Mock
    private ContentItemService contentService;

    @Mock
    private DomainService domainService;

    @InjectMocks
    private ContentItemController controller;

    @Test
    void testGetContentItems() {
        UUID domainId = UUID.randomUUID();
        ContentItem item = new ContentItem();
        item.setId(UUID.randomUUID());
        item.setTitle("Title");

        when(contentService.findAll(eq(domainId), any(), any(), eq(true), anyInt(), anyInt()))
                .thenReturn(List.of(item));

        List<ContentDtos.ContentItemResponse> result = controller.getContentItems(domainId, "video", 0, 10);
        assertEquals(1, result.size());
        assertEquals("Title", result.get(0).getTitle());
    }

    @Test
    void testCreateContentItem_Success() {
        UUID domainId = UUID.randomUUID();
        ContentDtos.ContentItemInput input = new ContentDtos.ContentItemInput();
        input.setDomainId(domainId);
        input.setTitle("New Item");

        Domain domain = new Domain();
        domain.setId(domainId);

        when(domainService.findById(domainId)).thenReturn(Optional.of(domain));
        when(contentService.create(any(ContentItem.class))).thenAnswer(i -> {
            ContentItem c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ResponseEntity<ContentItem> response = controller.createContentItem(input);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void testCreateContentItem_DomainNotFound() {
        UUID domainId = UUID.randomUUID();
        ContentDtos.ContentItemInput input = new ContentDtos.ContentItemInput();
        input.setDomainId(domainId);

        when(domainService.findById(domainId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> controller.createContentItem(input));
    }

    @Test
    void testGenerateContent() {
        UUID domainId = UUID.randomUUID();
        ContentDtos.GenerateContentInput input = new ContentDtos.GenerateContentInput();
        input.setDomainId(domainId);
        input.setNLessons(5);
        input.setTopic("Math");

        when(contentService.generateAndSave(domainId, 5, "Math")).thenReturn(Collections.emptyList());

        ResponseEntity<List<ContentItem>> response = controller.generateContent(input);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(contentService).generateAndSave(domainId, 5, "Math");
    }

    @Test
    void testGetContentItem_Found() {
        UUID id = UUID.randomUUID();
        ContentItem item = new ContentItem();
        item.setId(id);

        when(contentService.findById(id)).thenReturn(Optional.of(item));

        ResponseEntity<ContentDtos.ContentItemResponse> response = controller.getContentItem(id);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void testGetContentItem_NotFound() {
        UUID id = UUID.randomUUID();
        when(contentService.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> controller.getContentItem(id));
    }

    @Test
    void testUpdateContentItem_Found() {
        UUID id = UUID.randomUUID();
        ContentDtos.ContentItemInput input = new ContentDtos.ContentItemInput();
        input.setTitle("Updated");

        ContentItem updated = new ContentItem();
        updated.setId(id);
        updated.setTitle("Updated");

        when(contentService.update(eq(id), any(ContentItem.class))).thenReturn(Optional.of(updated));

        ResponseEntity<ContentDtos.ContentItemResponse> response = controller.updateContentItem(id, input);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated", response.getBody().getTitle());
    }

    @Test
    void testUpdateContentItem_NotFound() {
        UUID id = UUID.randomUUID();
        when(contentService.update(eq(id), any(ContentItem.class))).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> controller.updateContentItem(id, new ContentDtos.ContentItemInput()));
    }

    @Test
    void testDeleteContentItem() {
        UUID id = UUID.randomUUID();
        doNothing().when(contentService).delete(id);

        ResponseEntity<Void> response = controller.deleteContentItem(id);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(contentService).delete(id);
    }

    @Test
    void addSkills_withValidInput_shouldReturnOk() {
        UUID contentItemId = UUID.randomUUID();
        ContentDtos.ContentItemSkillInput input = new ContentDtos.ContentItemSkillInput();
        input.setSkillId(UUID.randomUUID());
        input.setWeight(1.0);

        // Assuming controller.addSkills returns ResponseEntity<?> based on typical
        // REST API patterns for successful updates
        ResponseEntity<?> response = controller.addSkills(contentItemId, Arrays.asList(input));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(contentService).updateSkillAssociations(eq(contentItemId), anyList(), anyList());
    }

    // -------------------------------------------------------------------------
    // POST /{id}/assessments/generate (US-10-08)
    // -------------------------------------------------------------------------

    @Test
    void testGenerateAssessments_ReturnsOk() {
        UUID id = UUID.randomUUID();
        ContentDtos.GenerateAssessmentsInput input = new ContentDtos.GenerateAssessmentsInput();
        input.setNItems(3);

        ContentDtos.AssessmentItemDraft draft = new ContentDtos.AssessmentItemDraft();
        draft.setQuestion("Q1");

        when(contentService.generateAssessments(id, 3)).thenReturn(List.of(draft));

        ResponseEntity<List<ContentDtos.AssessmentItemDraft>> response = controller.generateAssessments(id, input);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Q1", response.getBody().get(0).getQuestion());
        verify(contentService).generateAssessments(id, 3);
    }

    // -------------------------------------------------------------------------
    // POST /{id}/skills/auto-link (US-10-09)
    // -------------------------------------------------------------------------

    @Test
    void testAutoLinkSkills_ReturnsLinkedSkills() {
        UUID id = UUID.randomUUID();
        com.learnsmart.content.model.Skill skill = new com.learnsmart.content.model.Skill();
        skill.setId(UUID.randomUUID());
        skill.setCode("BIO-001");

        when(contentService.autoLinkSkills(id)).thenReturn(List.of(skill));

        ResponseEntity<List<com.learnsmart.content.model.Skill>> response = controller.autoLinkSkills(id);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("BIO-001", response.getBody().get(0).getCode());
        verify(contentService).autoLinkSkills(id);
    }

    // -------------------------------------------------------------------------
    // toDto — item with a non-null domain fills the domain DTO branch
    // -------------------------------------------------------------------------

    @Test
    void testGetContentItems_WithDomain_FillsDomainDto() {
        UUID domainId = UUID.randomUUID();

        com.learnsmart.content.model.Domain domain = new com.learnsmart.content.model.Domain();
        domain.setCode("MATH");
        domain.setName("Mathematics");

        ContentItem item = new ContentItem();
        item.setId(UUID.randomUUID());
        item.setTitle("Item with domain");
        item.setDomain(domain);

        when(contentService.findAll(eq(domainId), any(), any(), eq(true), anyInt(), anyInt()))
                .thenReturn(List.of(item));

        List<ContentDtos.ContentItemResponse> result = controller.getContentItems(domainId, null, 0, 10);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getDomain());
        assertEquals("MATH", result.get(0).getDomain().getCode());
    }
}
