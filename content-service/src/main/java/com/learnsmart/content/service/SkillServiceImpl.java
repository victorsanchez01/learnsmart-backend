package com.learnsmart.content.service;

import com.learnsmart.content.model.Skill;
import com.learnsmart.content.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;

    @Override
    public List<Skill> findAll(UUID domainId, String code, String search, Integer page, Integer size) {
        if (domainId != null) {
            return skillRepository.findByDomainId(domainId);
        }
        if (code != null) {
            return skillRepository.findByCodeContaining(code);
        }
        return skillRepository.findAll();
    }

    @Override
    public Optional<Skill> findById(UUID id) {
        return skillRepository.findById(id);
    }

    @Override
    @Transactional
    public Skill create(Skill skill) {
        return skillRepository.save(skill);
    }

    @Override
    @Transactional
    public Optional<Skill> update(UUID id, Skill skill) {
        return skillRepository.findById(id).map(existing -> {
            existing.setName(skill.getName());
            existing.setDescription(skill.getDescription());
            existing.setLevel(skill.getLevel());
            existing.setTags(skill.getTags());
            return skillRepository.save(existing);
        });
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        skillRepository.deleteById(id);
    }

    @Override
    public List<Skill> getPrerequisites(UUID id) {
        return skillRepository.findById(id)
                .map(s -> List.copyOf(s.getPrerequisites()))
                .orElse(Collections.emptyList());
    }

    @Override
    @Transactional
    public void updatePrerequisites(UUID id, List<UUID> prerequisiteIds) {
        Skill skill = skillRepository.findById(id).orElseThrow(() -> new RuntimeException("Skill not found"));
        List<Skill> prereqs = skillRepository.findAllById(prerequisiteIds);
        skill.setPrerequisites(new HashSet<>(prereqs));
        skillRepository.save(skill);
    }
}
