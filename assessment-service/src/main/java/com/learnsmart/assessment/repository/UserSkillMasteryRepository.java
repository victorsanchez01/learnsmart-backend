package com.learnsmart.assessment.repository;

import com.learnsmart.assessment.model.UserSkillMastery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface UserSkillMasteryRepository
        extends JpaRepository<UserSkillMastery, UserSkillMastery.UserSkillMasteryId> {
    List<UserSkillMastery> findByIdUserId(UUID userId);
}
