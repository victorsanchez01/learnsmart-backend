package com.learnsmart.profile.repository;

import com.learnsmart.profile.model.UserStudyPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface UserStudyPreferencesRepository extends JpaRepository<UserStudyPreferences, UUID> {
}
