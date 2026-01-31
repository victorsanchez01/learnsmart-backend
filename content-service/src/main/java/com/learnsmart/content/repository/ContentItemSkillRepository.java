package com.learnsmart.content.repository;

import com.learnsmart.content.model.ContentItemSkill;
import com.learnsmart.content.model.ContentItemSkill.ContentItemSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentItemSkillRepository extends JpaRepository<ContentItemSkill, ContentItemSkillId> {
}
