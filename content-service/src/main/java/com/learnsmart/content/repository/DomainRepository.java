package com.learnsmart.content.repository;

import com.learnsmart.content.model.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface DomainRepository extends JpaRepository<Domain, UUID> {
    Optional<Domain> findByCode(String code);
}
