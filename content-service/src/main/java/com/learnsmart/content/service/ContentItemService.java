package com.learnsmart.content.service;

import com.learnsmart.content.model.ContentItem;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface ContentItemService {
    List<ContentItem> findAll(UUID domainId, UUID skillId, String type, Boolean active, Integer page, Integer size);

    Optional<ContentItem> findById(UUID id);

    ContentItem create(ContentItem contentItem);

    Optional<ContentItem> update(UUID id, ContentItem contentItem);

    void delete(UUID id);

    // Skill relations
    void updateSkillAssociations(UUID contentItemId, List<UUID> skillIds, List<Double> weights);

    List<ContentItem> generateAndSave(UUID domainId, int nLessons, String topic);
}
