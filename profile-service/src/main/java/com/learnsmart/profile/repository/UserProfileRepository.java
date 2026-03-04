package com.learnsmart.profile.repository;

import com.learnsmart.profile.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByEmail(String email);

    Optional<UserProfile> findFirstByAuthUserId(String authUserId);
}
