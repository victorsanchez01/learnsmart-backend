package com.learnsmart.content.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.time.OffsetDateTime;

@Entity
@Table(name = "skills", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "domain_id", "code" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String level;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "skill_tags", joinColumns = @JoinColumn(name = "skill_id"))
    @Column(name = "tag")
    private List<String> tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToMany
    @JoinTable(name = "skill_prerequisites", joinColumns = @JoinColumn(name = "skill_id"), inverseJoinColumns = @JoinColumn(name = "prerequisite_id"))
    private Set<Skill> prerequisites;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
