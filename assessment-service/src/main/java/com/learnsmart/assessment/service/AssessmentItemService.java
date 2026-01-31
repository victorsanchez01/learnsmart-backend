package com.learnsmart.assessment.service;

import com.learnsmart.assessment.model.AssessmentItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentItemService {
    AssessmentItem create(AssessmentItem item);

    Optional<AssessmentItem> findById(UUID id);

    List<AssessmentItem> findAll();
}
