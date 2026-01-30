package com.learnsmart.content.service;

import com.learnsmart.content.model.Skill;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface SkillService {
    List<Skill> findAll(UUID domainId, String code, String search, Integer page, Integer size);

    Optional<Skill> findById(UUID id);

    Skill create(Skill skill);

    Optional<Skill> update(UUID id, Skill skill);

    void delete(UUID id);

    List<Skill> getPrerequisites(UUID id);

    void updatePrerequisites(UUID id, List<UUID> prerequisiteIds);
}
