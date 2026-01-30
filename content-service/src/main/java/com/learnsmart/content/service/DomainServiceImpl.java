package com.learnsmart.content.service;

import com.learnsmart.content.model.Domain;
import com.learnsmart.content.repository.DomainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DomainServiceImpl implements DomainService {

    private final DomainRepository domainRepository;

    @Override
    public List<Domain> findAll(String code, Integer page, Integer size) {
        // TFM Simplification: Pagination ignored in memory for MVP if repo doesn't
        // support complex specs yet,
        // or just returning all. Given requirements, let's return all or filter by
        // code.
        // Proper implementation would use Pageable.
        if (code != null) {
            Optional<Domain> d = domainRepository.findByCode(code);
            return d.map(List::of).orElse(List.of());
        }
        return domainRepository.findAll();
    }

    @Override
    public Optional<Domain> findById(UUID id) {
        return domainRepository.findById(id);
    }

    @Override
    @Transactional
    public Domain create(Domain domain) {
        if (domainRepository.findByCode(domain.getCode()).isPresent()) {
            throw new IllegalArgumentException("Domain code already exists: " + domain.getCode());
        }
        return domainRepository.save(domain);
    }

    @Override
    @Transactional
    public Optional<Domain> update(UUID id, Domain domain) {
        return domainRepository.findById(id).map(existing -> {
            existing.setName(domain.getName());
            existing.setDescription(domain.getDescription());
            // Code typically not updateable or requires check
            return domainRepository.save(existing);
        });
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        domainRepository.deleteById(id);
    }
}
