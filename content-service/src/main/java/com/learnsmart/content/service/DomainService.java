package com.learnsmart.content.service;

import com.learnsmart.content.model.Domain;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface DomainService {
    List<Domain> findAll(String code, Integer page, Integer size);

    Optional<Domain> findById(UUID id);

    Domain create(Domain domain);

    Optional<Domain> update(UUID id, Domain domain);

    void delete(UUID id);
}
