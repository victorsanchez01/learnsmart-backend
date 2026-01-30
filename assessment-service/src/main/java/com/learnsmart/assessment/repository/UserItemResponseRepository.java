package com.learnsmart.assessment.repository;

import com.learnsmart.assessment.model.UserItemResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface UserItemResponseRepository extends JpaRepository<UserItemResponse, UUID> {
    List<UserItemResponse> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
