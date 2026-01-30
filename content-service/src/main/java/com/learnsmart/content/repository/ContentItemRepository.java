package com.learnsmart.content.repository;

import com.learnsmart.content.model.ContentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {
    List<ContentItem> findByDomainId(UUID domainId);

    List<ContentItem> findByType(String type);
}
