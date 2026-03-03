package com.learnsmart.content.controller;

import com.learnsmart.content.dto.ContentDtos;
import com.learnsmart.content.model.Domain;
import com.learnsmart.content.model.Skill;
import com.learnsmart.content.service.DomainService;
import com.learnsmart.content.service.SkillService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillControllerTest {

    @Mock
    private SkillService skillService;

    @Mock
    private DomainService domainService;

    @InjectMocks
    private SkillController controller;

    // -------------------------------------------------------------------------
    // GET /skills
    // -------------------------------------------------------------------------

    @Test
    void testGetSkills() {
        UUID domainId = UUID.randomUUID();
        when(skillService.findAll(eq(domainId), any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        List<Skill> result = controller.getSkills(domainId, 0, 10);
        assertTrue(result.isEmpty());
        verify(skillService).findAll(eq(domainId), any(), eq(0), eq(10));
    }

    // -------------------------------------------------------------------------
    // GET /skills/{id}
    // -------------------------------------------------------------------------

    @Test
    void testGetSkill_Found() {
        UUID id = UUID.randomUUID();
        Skill skill = new Skill();
        skill.setId(id);
        when(skillService.findById(id)).thenReturn(Optional.of(skill));

        ResponseEntity<Skill> response = controller.getSkill(id);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void testGetSkill_NotFound() {
        UUID id = UUID.randomUUID();
        when(skillService.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<Skill> response = controller.getSkill(id);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // POST /skills
    // -------------------------------------------------------------------------

    @Test
    void testCreateSkill_Success() {
        UUID domainId = UUID.randomUUID();
        ContentDtos.SkillInput input = new ContentDtos.SkillInput();
        input.setDomainId(domainId);
        input.setCode("ALG-001");

        Domain domain = new Domain();
        domain.setId(domainId);

        when(domainService.findById(domainId)).thenReturn(Optional.of(domain));
        when(skillService.create(any(Skill.class))).thenAnswer(i -> {
            Skill s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        ResponseEntity<Skill> response = controller.createSkill(input);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void testCreateSkill_DomainNotFound() {
        UUID domainId = UUID.randomUUID();
        ContentDtos.SkillInput input = new ContentDtos.SkillInput();
        input.setDomainId(domainId);

        when(domainService.findById(domainId)).thenReturn(Optional.empty());

        assertThrows(com.learnsmart.content.exception.DomainNotFoundException.class,
                () -> controller.createSkill(input));
    }

    // -------------------------------------------------------------------------
    // PUT /skills/{id}
    // -------------------------------------------------------------------------

    @Test
    void testUpdateSkill_Found() {
        UUID id = UUID.randomUUID();
        ContentDtos.SkillInput input = new ContentDtos.SkillInput();
        input.setName("Updated Name");

        Skill updated = new Skill();
        updated.setId(id);
        updated.setName("Updated Name");

        when(skillService.update(eq(id), any(Skill.class))).thenReturn(Optional.of(updated));

        ResponseEntity<Skill> response = controller.updateSkill(id, input);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated Name", response.getBody().getName());
    }

    @Test
    void testUpdateSkill_NotFound() {
        UUID id = UUID.randomUUID();
        when(skillService.update(eq(id), any(Skill.class))).thenReturn(Optional.empty());

        ResponseEntity<Skill> response = controller.updateSkill(id, new ContentDtos.SkillInput());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // DELETE /skills/{id}
    // -------------------------------------------------------------------------

    @Test
    void testDeleteSkill() {
        UUID id = UUID.randomUUID();
        doNothing().when(skillService).delete(id);

        ResponseEntity<Void> response = controller.delete(id);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(skillService).delete(id);
    }

    // -------------------------------------------------------------------------
    // GET /skills/{id}/prerequisites
    // -------------------------------------------------------------------------

    @Test
    void testGetPrerequisites_ReturnsDtos() {
        UUID id = UUID.randomUUID();
        UUID prereqId = UUID.randomUUID();

        Skill prereq = new Skill();
        prereq.setId(prereqId);
        prereq.setName("Prereq");
        prereq.setLevel("beginner");

        when(skillService.getPrerequisites(id)).thenReturn(List.of(prereq));

        ResponseEntity<List<ContentDtos.SkillDto>> response = controller.getPrerequisites(id);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(prereqId, response.getBody().get(0).getId());
    }

    // -------------------------------------------------------------------------
    // PUT /skills/{id}/prerequisites
    // -------------------------------------------------------------------------

    @Test
    void testUpdatePrerequisites() {
        UUID id = UUID.randomUUID();
        UUID prereqId = UUID.randomUUID();

        doNothing().when(skillService).updatePrerequisites(eq(id), anyList());

        ResponseEntity<Void> response = controller.updatePrerequisites(id, List.of(prereqId));
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(skillService).updatePrerequisites(eq(id), anyList());
    }

    // -------------------------------------------------------------------------
    // POST /skills/domains/{domainId}/generate
    // -------------------------------------------------------------------------

    @Test
    void testGenerateSkills_ReturnCreated() {
        UUID domainId = UUID.randomUUID();
        ContentDtos.GenerateSkillsInput input = new ContentDtos.GenerateSkillsInput();
        input.setTopic("algebra");

        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setCode("ALG-001");

        when(skillService.generateSkills(domainId, "algebra")).thenReturn(List.of(skill));

        ResponseEntity<List<Skill>> response = controller.generateSkills(domainId, input);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(skillService).generateSkills(domainId, "algebra");
    }

    // -------------------------------------------------------------------------
    // POST /skills/domains/{domainId}/link
    // -------------------------------------------------------------------------

    @Test
    void testLinkSkills_ReturnsNoContent() {
        UUID domainId = UUID.randomUUID();
        doNothing().when(skillService).linkSkills(domainId);

        ResponseEntity<Void> response = controller.linkSkills(domainId);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(skillService).linkSkills(domainId);
    }
}
