package com.learnsmart.content.repository;

import com.learnsmart.content.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findByDomainId(UUID domainId);

    List<Skill> findByCodeContaining(String code);
}
