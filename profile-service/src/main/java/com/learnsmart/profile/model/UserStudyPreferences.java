package com.learnsmart.profile.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_study_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStudyPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "hours_per_week")
    private Double hoursPerWeek;

    @ElementCollection
    @CollectionTable(name = "user_preferred_days", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "day")
    private List<String> preferredDays;

    @Column(name = "preferred_session_minutes")
    private Integer preferredSessionMinutes;

    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled;
}
