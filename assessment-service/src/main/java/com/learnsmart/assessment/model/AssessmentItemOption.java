package com.learnsmart.assessment.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "assessment_item_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_item_id", nullable = false)
    private AssessmentItem assessmentItem;

    @Column(length = 10)
    private String label;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;

    @Column(name = "error_tag", length = 100)
    private String errorTag;

    @Column(name = "feedback_template", columnDefinition = "TEXT")
    private String feedbackTemplate;
}
