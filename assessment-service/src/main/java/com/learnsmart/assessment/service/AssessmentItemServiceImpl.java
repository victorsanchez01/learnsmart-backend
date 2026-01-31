package com.learnsmart.assessment.service;

import com.learnsmart.assessment.model.AssessmentItem;
import com.learnsmart.assessment.repository.AssessmentItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssessmentItemServiceImpl implements AssessmentItemService {

    private final AssessmentItemRepository assessmentItemRepository;

    @Override
    @Transactional
    public AssessmentItem create(AssessmentItem item) {
        // Link bidirectional relationships
        if (item.getSkills() != null) {
            item.getSkills().forEach(s -> s.setAssessmentItem(item));
        }
        if (item.getOptions() != null) {
            item.getOptions().forEach(o -> o.setAssessmentItem(item));
        }
        return assessmentItemRepository.save(item);
    }

    @Override
    public Optional<AssessmentItem> findById(UUID id) {
        return assessmentItemRepository.findById(id);
    }

    @Override
    public List<AssessmentItem> findAll() {
        return assessmentItemRepository.findAll();
    }
}
