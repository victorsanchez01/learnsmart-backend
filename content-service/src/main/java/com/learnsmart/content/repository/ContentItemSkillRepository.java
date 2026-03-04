package com.learnsmart.content.repository;

import com.learnsmart.content.model.ContentItemSkill;
import com.learnsmart.content.model.ContentItemSkill.ContentItemSkillId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ContentItemSkillRepository extends JpaRepository<ContentItemSkill, ContentItemSkillId> {
    @Modifying
    @Transactional
    @Query("DELETE FROM ContentItemSkill c WHERE c.id.contentItemId = :contentItemId")
    void deleteByIdContentItemId(@Param("contentItemId") UUID contentItemId);
}
