package com.learnsmart.content.service;

import com.learnsmart.content.model.ContentItem;
import com.learnsmart.content.model.ContentItemSkill;
import com.learnsmart.content.model.Skill;
import com.learnsmart.content.repository.ContentItemRepository;
import com.learnsmart.content.repository.SkillRepository;
import com.learnsmart.content.repository.DomainRepository;
import com.learnsmart.content.repository.ContentItemSkillRepository;
import com.learnsmart.content.client.AiServiceClient;
import com.learnsmart.content.dto.AiDtos;
import com.learnsmart.content.model.Domain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ContentItemServiceImpl implements ContentItemService {

    private final ContentItemRepository contentItemRepository;
    // Assuming we might need a separate repository for ContentItemSkill if managing
    // explicitly,
    // but JPA Cascade often handles it. For now, focus on core item.
    private final SkillRepository skillRepository;
    private final ContentItemSkillRepository contentItemSkillRepository;
    private final AiServiceClient aiServiceClient;
    private final DomainRepository domainRepository;

    @Override
    public List<ContentItem> findAll(UUID domainId, UUID skillId, String type, Boolean active, Integer page,
            Integer size) {
        if (domainId != null) {
            return contentItemRepository.findByDomainId(domainId);
        }
        if (type != null) {
            return contentItemRepository.findByType(type);
        }
        // Metadata filter for skillId is complex without Spec. Ignoring for MVP step.
        return contentItemRepository.findAll();
    }

    @Override
    public Optional<ContentItem> findById(UUID id) {
        return contentItemRepository.findById(id);
    }

    @Override
    @Transactional
    public ContentItem create(ContentItem contentItem) {
        return contentItemRepository.save(contentItem);
    }

    @Override
    @Transactional
    public Optional<ContentItem> update(UUID id, ContentItem contentItem) {
        return contentItemRepository.findById(id).map(existing -> {
            existing.setTitle(contentItem.getTitle());
            existing.setDescription(contentItem.getDescription());
            existing.setEstimatedMinutes(contentItem.getEstimatedMinutes());
            existing.setDifficulty(contentItem.getDifficulty());
            existing.setMetadata(contentItem.getMetadata());
            existing.setActive(contentItem.isActive());
            return contentItemRepository.save(existing);
        });
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        contentItemSkillRepository.deleteByIdContentItemId(id);
        contentItemRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateSkillAssociations(UUID contentItemId, List<UUID> skillIds, List<Double> weights) {
        ContentItem item = contentItemRepository.findById(contentItemId)
                .orElseThrow(() -> new RuntimeException("ContentItem not found: " + contentItemId));

        // CRITICAL: Clear existing associations first to avoid PK violations
        contentItemSkillRepository.deleteByIdContentItemId(contentItemId);
        contentItemSkillRepository.flush();

        if (skillIds != null && !skillIds.isEmpty()) {
            for (int i = 0; i < skillIds.size(); i++) {
                UUID skillId = skillIds.get(i);
                if (skillId == null)
                    continue;

                Double weightVal = (weights != null && weights.size() > i) ? weights.get(i) : 1.0;
                if (weightVal == null)
                    weightVal = 1.0;
                BigDecimal weight = BigDecimal.valueOf(weightVal);

                com.learnsmart.content.model.Skill skill = skillRepository.findById(skillId)
                        .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

                ContentItemSkill cis = new ContentItemSkill();
                cis.setId(new ContentItemSkill.ContentItemSkillId(contentItemId, skillId));
                cis.setContentItem(item);
                cis.setSkill(skill);
                cis.setWeight(weight);
                contentItemSkillRepository.save(cis);
            }
            contentItemSkillRepository.flush();
        }
    }

    @Override
    @Transactional
    public List<ContentItem> generateAndSave(UUID domainId, int nLessons, String topic) {
        Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new RuntimeException("Domain not found"));

        AiDtos.GenerateLessonsRequest request = AiDtos.GenerateLessonsRequest.builder()
                .domainId(domain.getName())
                .skillIds(List.of(topic)) // Using topic as a skill tag
                .nLessons(nLessons)
                .level("beginner")
                .difficulty(0.5)
                .locale("es-ES") // Defaulting to Spanish per user context
                .build();

        AiDtos.GenerateLessonsResponse response = aiServiceClient.generateLessons(request);

        return response.getLessons().stream().map(draft -> {
            ContentItem item = new ContentItem();
            item.setDomain(domain);
            item.setTitle(draft.getTitle());
            item.setDescription(draft.getDescription());
            item.setEstimatedMinutes(draft.getEstimatedMinutes());
            item.setDifficulty(BigDecimal.valueOf(draft.getDifficulty() != null ? draft.getDifficulty() : 0.5));
            item.setType(draft.getType() != null ? draft.getType() : "lesson");
            item.setMetadata(java.util.Map.of("body", draft.getBody() != null ? draft.getBody() : ""));
            item.setActive(true);
            return contentItemRepository.save(item);
        }).toList();
    }

    // US-10-08: AI Assessment Item Generation
    @Override
    @Transactional(readOnly = true)
    public List<com.learnsmart.content.dto.ContentDtos.AssessmentItemDraft> generateAssessments(UUID contentItemId,
            int nItems) {
        ContentItem item = contentItemRepository.findById(contentItemId)
                .orElseThrow(() -> new RuntimeException("ContentItem not found: " + contentItemId));

        // Extract body text from metadata
        String contextText = item.getMetadata() != null && item.getMetadata().containsKey("body")
                ? item.getMetadata().get("body").toString()
                : item.getDescription();

        // Call AI service
        AiDtos.GenerateAssessmentItemsRequest request = AiDtos.GenerateAssessmentItemsRequest.builder()
                .contextText(contextText)
                .nItems(nItems)
                .domainId(item.getDomain().getName()) // Pass domain name, not UUID
                .build();

        AiDtos.GenerateAssessmentItemsResponse response = aiServiceClient.generateAssessmentItems(request);

        // Convert AI DTOs to Content DTOs
        return response.getItems().stream()
                .map(aiItem -> {
                    com.learnsmart.content.dto.ContentDtos.AssessmentItemDraft draft = new com.learnsmart.content.dto.ContentDtos.AssessmentItemDraft();
                    draft.setQuestion(aiItem.getQuestion());
                    draft.setOptions(aiItem.getOptions());
                    draft.setCorrectIndex(aiItem.getCorrectIndex());
                    draft.setExplanation(aiItem.getExplanation());
                    draft.setDifficulty(aiItem.getDifficulty());
                    return draft;
                })
                .toList();
    }

    // US-10-09: AI Skill Tagging
    @Override
    @Transactional
    public List<Skill> autoLinkSkills(UUID contentItemId) {
        ContentItem item = contentItemRepository.findById(contentItemId)
                .orElseThrow(() -> new RuntimeException("ContentItem not found: " + contentItemId));

        // Extract content text from metadata
        String contentText = item.getMetadata() != null && item.getMetadata().containsKey("body")
                ? item.getMetadata().get("body").toString()
                : item.getDescription();

        // Call AI service
        AiDtos.AnalyzeSkillTagsRequest request = AiDtos.AnalyzeSkillTagsRequest.builder()
                .contentText(contentText)
                .domainId(item.getDomain().getName()) // Pass domain name, not UUID
                .build();

        AiDtos.AnalyzeSkillTagsResponse response = aiServiceClient.analyzeSkillTags(request);

        // Find skills by code in the domain
        List<Skill> domainSkills = skillRepository.findByDomainId(item.getDomain().getId());
        List<Skill> matchedSkills = domainSkills.stream()
                .filter(skill -> response.getSuggestedSkillCodes().contains(skill.getCode()))
                .toList();

        // Link skills to content item (clear existing and add new)
        // TODO: Add deleteByContentItemId method to ContentItemSkillRepository
        // contentItemSkillRepository.deleteByContentItemId(contentItemId);
        for (int i = 0; i < matchedSkills.size(); i++) {
            Skill skill = matchedSkills.get(i);
            ContentItemSkill cis = new ContentItemSkill();
            cis.setId(new ContentItemSkill.ContentItemSkillId(contentItemId, skill.getId()));
            cis.setContentItem(item);
            cis.setSkill(skill);
            cis.setWeight(BigDecimal.valueOf(1.0)); // Default weight
            contentItemSkillRepository.saveAndFlush(cis);
        }

        return matchedSkills;
    }
}
