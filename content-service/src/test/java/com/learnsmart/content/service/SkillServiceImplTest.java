package com.learnsmart.content.service;

import com.learnsmart.content.client.AiServiceClient;
import com.learnsmart.content.dto.AiDtos;
import com.learnsmart.content.model.Domain;
import com.learnsmart.content.model.Skill;
import com.learnsmart.content.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {

    @Mock
    private SkillRepository skillRepository;
    @Mock
    private DomainService domainService;
    @Mock
    private AiServiceClient aiServiceClient;

    @InjectMocks
    private SkillServiceImpl skillService;

    @Test
    void testFindAll_WithDomainId() {
        UUID domainId = UUID.randomUUID();
        when(skillRepository.findByDomainId(domainId)).thenReturn(Collections.emptyList());

        List<Skill> result = skillService.findAll(domainId, null, 0, 10);
        assertNotNull(result);
        verify(skillRepository).findByDomainId(domainId);
    }

    @Test
    void testFindAll_NoFilters() {
        when(skillRepository.findAll()).thenReturn(Collections.emptyList());
        List<Skill> result = skillService.findAll(null, null, 0, 10);
        assertNotNull(result);
        verify(skillRepository).findAll();
    }

    @Test
    void testFindById() {
        UUID id = UUID.randomUUID();
        Skill skill = new Skill();
        skill.setId(id);
        when(skillRepository.findById(id)).thenReturn(Optional.of(skill));

        Optional<Skill> result = skillService.findById(id);
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void testCreate() {
        Skill skill = new Skill();
        when(skillRepository.save(skill)).thenReturn(skill);

        Skill result = skillService.create(skill);
        assertNotNull(result);
        verify(skillRepository).save(skill);
    }

    @Test
    void testUpdate_Found() {
        UUID id = UUID.randomUUID();
        Skill existing = new Skill();
        existing.setId(id);
        Skill update = new Skill();
        update.setName("New Name");

        when(skillRepository.findById(id)).thenReturn(Optional.of(existing));
        when(skillRepository.save(existing)).thenReturn(existing);

        Optional<Skill> result = skillService.update(id, update);
        assertTrue(result.isPresent());
        assertEquals("New Name", result.get().getName());
        verify(skillRepository).save(existing);
    }

    @Test
    void testUpdate_NotFound() {
        UUID id = UUID.randomUUID();
        when(skillRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Skill> result = skillService.update(id, new Skill());
        assertFalse(result.isPresent());
    }

    @Test
    void testDelete() {
        UUID id = UUID.randomUUID();
        doNothing().when(skillRepository).deleteById(id);
        skillService.delete(id);
        verify(skillRepository).deleteById(id);
    }

    @Test
    void testGetPrerequisites() {
        UUID id = UUID.randomUUID();
        Skill skill = new Skill();
        Skill prereq = new Skill();
        skill.setPrerequisites(Set.of(prereq));

        when(skillRepository.findById(id)).thenReturn(Optional.of(skill));

        List<Skill> result = skillService.getPrerequisites(id);
        assertEquals(1, result.size());
    }

    @Test
    void testUpdatePrerequisites() {
        UUID id = UUID.randomUUID();
        UUID prereqId = UUID.randomUUID();
        Skill skill = new Skill();
        skill.setId(id);
        skill.setPrerequisites(new HashSet<>());

        Skill prereq = new Skill();
        prereq.setId(prereqId);
        prereq.setPrerequisites(new HashSet<>());

        when(skillRepository.findById(id)).thenReturn(Optional.of(skill));
        when(skillRepository.findAllById(List.of(prereqId))).thenReturn(List.of(prereq));
        when(skillRepository.save(skill)).thenReturn(skill);

        skillService.updatePrerequisites(id, List.of(prereqId));

        assertEquals(1, skill.getPrerequisites().size());
        assertTrue(skill.getPrerequisites().contains(prereq));
        verify(skillRepository).save(skill);
    }

    @Test
    void testUpdatePrerequisites_SkillNotFound() {
        UUID id = UUID.randomUUID();
        when(skillRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> skillService.updatePrerequisites(id, List.of()));
    }

    @Test
    void testUpdatePrerequisites_SelfReference_Throws() {
        // A skill cannot list itself as a prerequisite (cycle of 0 steps).
        UUID id = UUID.randomUUID();
        Skill skill = new Skill();
        skill.setId(id);
        skill.setPrerequisites(new HashSet<>());

        when(skillRepository.findById(id)).thenReturn(Optional.of(skill));

        assertThrows(IllegalArgumentException.class,
                () -> skillService.updatePrerequisites(id, List.of(id)),
                "Skill cannot depend on itself");
        verify(skillRepository, never()).save(any());
    }

    @Test
    void testUpdatePrerequisites_PrerequisiteNotFound_Throws() {
        // If one of the provided prerequisite IDs does not exist the service must
        // throw.
        UUID id = UUID.randomUUID();
        UUID missingPrereqId = UUID.randomUUID();
        Skill skill = new Skill();
        skill.setId(id);
        skill.setPrerequisites(new HashSet<>());

        when(skillRepository.findById(id)).thenReturn(Optional.of(skill));
        // findAllById returns empty — none of the requested IDs exist
        when(skillRepository.findAllById(List.of(missingPrereqId))).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> skillService.updatePrerequisites(id, List.of(missingPrereqId)),
                "Service must throw when prerequisite IDs are not found");
        verify(skillRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getPrerequisites — skill not found returns empty list
    // -------------------------------------------------------------------------

    @Test
    void testGetPrerequisites_SkillNotFound_ReturnsEmptyList() {
        UUID id = UUID.randomUUID();
        when(skillRepository.findById(id)).thenReturn(Optional.empty());

        List<Skill> result = skillService.getPrerequisites(id);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // updatePrerequisites — cyclic dependency detected
    // -------------------------------------------------------------------------

    @Test
    void testUpdatePrerequisites_CycleDetected_Throws() {
        UUID skillAId = UUID.randomUUID();
        UUID skillBId = UUID.randomUUID();

        Skill skillA = new Skill();
        skillA.setId(skillAId);
        skillA.setName("Skill A");

        Skill skillB = new Skill();
        skillB.setId(skillBId);
        skillB.setName("Skill B");
        // skillB depends on skillA → adding skillB as prereq of skillA creates a cycle
        skillB.setPrerequisites(Set.of(skillA));

        skillA.setPrerequisites(new HashSet<>());

        when(skillRepository.findById(skillAId)).thenReturn(Optional.of(skillA));
        when(skillRepository.findAllById(List.of(skillBId))).thenReturn(List.of(skillB));

        assertThrows(IllegalArgumentException.class,
                () -> skillService.updatePrerequisites(skillAId, List.of(skillBId)),
                "Circular dependency must be detected");
        verify(skillRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // generateSkills — happy path: domain found, AI returns drafts
    // -------------------------------------------------------------------------

    @Test
    void testGenerateSkills_DomainFound_CreatesSkills() {
        UUID domainId = UUID.randomUUID();

        Domain domain = new Domain();
        domain.setId(domainId);
        domain.setName("Math");

        AiDtos.SkillDraft draft = AiDtos.SkillDraft.builder()
                .code("MATH-001")
                .name("Algebra Basics")
                .description("Intro to algebra")
                .level("beginner")
                .tags(List.of("algebra"))
                .build();

        AiDtos.GenerateSkillsResponse aiResponse = AiDtos.GenerateSkillsResponse.builder()
                .skills(List.of(draft))
                .build();

        when(domainService.findById(domainId)).thenReturn(Optional.of(domain));
        when(aiServiceClient.generateSkills(any(AiDtos.GenerateSkillsRequest.class))).thenReturn(aiResponse);
        when(skillRepository.save(any(Skill.class))).thenAnswer(i -> i.getArgument(0));

        List<Skill> result = skillService.generateSkills(domainId, "algebra");

        assertEquals(1, result.size());
        assertEquals("MATH-001", result.get(0).getCode());
        verify(skillRepository).save(any(Skill.class));
    }

    // -------------------------------------------------------------------------
    // generateSkills — domain not found throws DomainNotFoundException
    // -------------------------------------------------------------------------

    @Test
    void testGenerateSkills_DomainNotFound_Throws() {
        UUID domainId = UUID.randomUUID();
        when(domainService.findById(domainId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> skillService.generateSkills(domainId, "anything"));
        verify(aiServiceClient, never()).generateSkills(any());
    }

    // -------------------------------------------------------------------------
    // linkSkills — happy path: AI suggests prereqs, skills are updated
    // -------------------------------------------------------------------------

    @Test
    void testLinkSkills_PrereqsLinked_SavesSkills() {
        UUID domainId = UUID.randomUUID();

        Skill skillA = new Skill();
        skillA.setId(UUID.randomUUID());
        skillA.setCode("A");
        skillA.setName("Skill A");
        skillA.setLevel("beginner");
        skillA.setTags(Collections.emptyList());
        skillA.setPrerequisites(new HashSet<>());

        Skill skillB = new Skill();
        skillB.setId(UUID.randomUUID());
        skillB.setCode("B");
        skillB.setName("Skill B");
        skillB.setLevel("intermediate");
        skillB.setTags(Collections.emptyList());
        skillB.setPrerequisites(new HashSet<>());

        AiDtos.PrerequisiteLink link = AiDtos.PrerequisiteLink.builder()
                .skillCode("B")
                .prerequisiteCodes(List.of("A"))
                .build();

        AiDtos.GeneratePrerequisitesResponse aiResponse = AiDtos.GeneratePrerequisitesResponse.builder()
                .prerequisites(List.of(link))
                .build();

        when(skillRepository.findByDomainId(domainId)).thenReturn(List.of(skillA, skillB));
        when(aiServiceClient.generatePrerequisites(any(AiDtos.GeneratePrerequisitesRequest.class)))
                .thenReturn(aiResponse);
        when(skillRepository.save(any(Skill.class))).thenAnswer(i -> i.getArgument(0));

        skillService.linkSkills(domainId);

        // skillB should now have skillA as a prerequisite
        assertTrue(skillB.getPrerequisites().contains(skillA));
        verify(skillRepository).save(skillB);
    }

    // -------------------------------------------------------------------------
    // linkSkills — empty domain throws IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test
    void testLinkSkills_EmptyDomain_Throws() {
        UUID domainId = UUID.randomUUID();
        when(skillRepository.findByDomainId(domainId)).thenReturn(Collections.emptyList());

        assertThrows(IllegalArgumentException.class,
                () -> skillService.linkSkills(domainId),
                "Should throw when no skills exist for the domain");
        verify(aiServiceClient, never()).generatePrerequisites(any());
    }
}
