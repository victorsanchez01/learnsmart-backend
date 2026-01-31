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
        contentItemRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateSkillAssociations(UUID contentItemId, List<UUID> skillIds, List<Double> weights) {
        ContentItem item = contentItemRepository.findById(contentItemId)
                .orElseThrow(() -> new RuntimeException("ContentItem not found"));

        for (int i = 0; i < skillIds.size(); i++) {
            UUID skillId = skillIds.get(i);
            Double weight = weights.get(i);
            Skill skill = skillRepository.findById(skillId)
                    .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

            ContentItemSkill cis = new ContentItemSkill();
            cis.setId(new ContentItemSkill.ContentItemSkillId(contentItemId, skillId));
            cis.setContentItem(item);
            cis.setSkill(skill);
            cis.setWeight(BigDecimal.valueOf(weight));
            contentItemSkillRepository.save(cis);
        }
    }

    @Override
    @Transactional
    public List<ContentItem> generateAndSave(UUID domainId, int nLessons, String topic) {
        Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new RuntimeException("Domain not found"));

        AiDtos.GenerateLessonsRequest request = AiDtos.GenerateLessonsRequest.builder()
                .domain(domain.getName())
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
}
