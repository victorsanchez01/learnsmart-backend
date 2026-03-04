package com.learnsmart.profile.service;

import com.learnsmart.profile.dto.ProfileDtos.*;
import com.learnsmart.profile.model.UserGoal;
import com.learnsmart.profile.model.UserProfile;
import com.learnsmart.profile.repository.UserGoalRepository;
import com.learnsmart.profile.repository.UserProfileRepository;
import com.learnsmart.profile.repository.UserStudyPreferencesRepository;
import com.learnsmart.profile.model.UserStudyPreferences;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl {

    private final UserProfileRepository profileRepository;
    private final UserGoalRepository goalRepository;
    private final UserStudyPreferencesRepository preferencesRepository;
    private final AuditService auditService;
    private final com.learnsmart.profile.client.ContentServiceClient contentClient;

    @Transactional
    public UserProfileResponse registerUser(UserRegistrationRequest request) {
        // Extract auth user ID from JWT token (if available)
        String authUserId = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            authUserId = jwt.getSubject();
        }

        // Fallback to simulated ID if no JWT present (for testing/legacy)
        if (authUserId == null) {
            authUserId = UUID.randomUUID().toString();
        }

        if (profileRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        UserProfile profile = UserProfile.builder()
                .userId(UUID.randomUUID())
                .authUserId(authUserId)
                .email(request.getEmail())
                .displayName(request.getDisplayName())
                .locale(request.getLocale())
                .timezone(request.getTimezone())
                .build();

        profile = profileRepository.save(profile);

        // US-094: Log profile creation
        auditService.logProfileChange(
                profile.getUserId(),
                profile.getUserId(), // User created their own profile
                "CREATE",
                null,
                profile,
                getCurrentRequest());

        return mapToProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return profileRepository.findById(userId)
                .map(this::mapToProfileResponse)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByAuthId(String authUserId) {
        return profileRepository.findFirstByAuthUserId(authUserId)
                .map(this::mapToProfileResponse)
                .orElseThrow(() -> new IllegalArgumentException("User not found for authUserId: " + authUserId));
    }

    // Método auxiliar para buscar por ID de usuario interno (simulo "me" si tuviera
    // contexto de seguridad)
    // Para esta prueba, asumiremos que el controller extrae el ID correcto.

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UserProfileUpdateRequest request) {
        UserProfile oldProfile = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Create a copy for audit trail
        UserProfile oldProfileCopy = UserProfile.builder()
                .userId(oldProfile.getUserId())
                .authUserId(oldProfile.getAuthUserId())
                .email(oldProfile.getEmail())
                .displayName(oldProfile.getDisplayName())
                .birthYear(oldProfile.getBirthYear())
                .locale(oldProfile.getLocale())
                .timezone(oldProfile.getTimezone())
                .createdAt(oldProfile.getCreatedAt())
                .updatedAt(oldProfile.getUpdatedAt())
                .build();

        if (request.getDisplayName() != null)
            oldProfile.setDisplayName(request.getDisplayName());
        if (request.getBirthYear() != null)
            oldProfile.setBirthYear(request.getBirthYear());
        if (request.getLocale() != null)
            oldProfile.setLocale(request.getLocale());
        if (request.getTimezone() != null)
            oldProfile.setTimezone(request.getTimezone());

        UserProfile updatedProfile = profileRepository.save(oldProfile);

        // US-094: Log profile update
        auditService.logProfileChange(
                userId,
                userId, // User updated their own profile
                "UPDATE",
                oldProfileCopy,
                updatedProfile,
                getCurrentRequest());

        return mapToProfileResponse(updatedProfile);
    }

    @Transactional(readOnly = true)
    public List<UserGoalResponse> getUserGoals(UUID userId) {
        return goalRepository.findByUserId(userId).stream()
                .map(this::mapToGoalResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserGoalResponse createGoal(UUID userId, UserGoalCreateRequest request) {
        UserGoal goal = UserGoal.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .domainId(request.getDomainId())
                .skillId(request.getSkillId()) // US-093
                .targetLevel(request.getTargetLevel())
                .dueDate(request.getDueDate())
                .intensity(request.getIntensity())
                .isActive(true)
                .build();

        // US-093: Validate Domain and Skill
        // US-093: Validate Domain and Skill
        if (request.getDomainId() != null) {
            try {
                // US-10-01: Use ID-based validation
                var domain = contentClient.getDomain(request.getDomainId());
                if (domain == null) {
                    throw new IllegalArgumentException("Domain not found: " + request.getDomainId());
                }
            } catch (Exception e) {
                // If not found (404) or other error, treat as validation failure for Strict
                // Mode
                if (e instanceof ResponseStatusException)
                    throw e;
                // Log the real error for debugging
                System.err.println("Domain validation failed: " + e.getMessage());
                // e.printStackTrace();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid domain: " + request.getDomainId() + " (Catalog service check failed: " + e.getMessage()
                                + ")");
            }
        }

        if (request.getSkillId() != null) {
            try {
                // Check if skill exists
                var skill = contentClient.getSkill(request.getSkillId());
                if (skill == null) {
                    throw new IllegalArgumentException("Skill not found: " + request.getSkillId());
                }
                // Optional: Check if skill belongs to domain if both provided
                if (request.getDomainId() != null && skill.getDomain() != null
                        && !request.getDomainId().equals(skill.getDomain().getId())) {
                    throw new IllegalArgumentException(
                            "Skill '" + skill.getCode() + "' does not belong to domain " + request.getDomainId());
                }
            } catch (Exception e) {
                if (e instanceof ResponseStatusException)
                    throw e;
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid skillId: " + request.getSkillId());
            }
        }

        goal = goalRepository.save(goal);

        // US-094: Log goal creation
        auditService.logGoalChange(
                userId,
                userId,
                "CREATE",
                null,
                goal,
                getCurrentRequest());

        return mapToGoalResponse(goal);
    }

    @Transactional
    public UserGoalResponse updateGoal(UUID userId, UUID goalId, UserGoalUpdateRequest request) {
        UserGoal oldGoal = goalRepository.findById(goalId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));

        // Create a copy for audit trail
        UserGoal oldGoalCopy = copyGoal(oldGoal);

        if (request.getTitle() != null)
            oldGoal.setTitle(request.getTitle());
        if (request.getDescription() != null)
            oldGoal.setDescription(request.getDescription());
        if (request.getDomainId() != null) {
            // US-093 Validation on Update
            try {
                var domain = contentClient.getDomain(request.getDomainId());
                if (domain == null) {
                    throw new IllegalArgumentException("Domain not found: " + request.getDomainId());
                }
            } catch (Exception e) {
                if (e instanceof ResponseStatusException)
                    throw e;
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid domain: " + request.getDomainId());
            }
            oldGoal.setDomainId(request.getDomainId());
        }
        if (request.getTargetLevel() != null)
            oldGoal.setTargetLevel(request.getTargetLevel());
        if (request.getDueDate() != null)
            oldGoal.setDueDate(request.getDueDate());
        if (request.getIntensity() != null)
            oldGoal.setIntensity(request.getIntensity());
        if (request.getIsActive() != null)
            oldGoal.setIsActive(request.getIsActive());

        // US-096: Handle status and completion percentage
        if (request.getStatus() != null) {
            oldGoal.setStatus(request.getStatus());
            // Auto-set completedAt if status is COMPLETED
            if ("COMPLETED".equalsIgnoreCase(request.getStatus()) && oldGoal.getCompletedAt() == null) {
                oldGoal.setCompletedAt(OffsetDateTime.now());
            }
        }
        if (request.getProgressPercentage() != null) {
            oldGoal.setCompletionPercentage(request.getProgressPercentage());
            // Auto-complete if progress reaches 100%
            if (request.getProgressPercentage() >= 100 && oldGoal.getCompletedAt() == null) {
                oldGoal.setStatus("COMPLETED");
                oldGoal.setCompletedAt(OffsetDateTime.now());
            }
        }

        UserGoal updatedGoal = goalRepository.save(oldGoal);

        // US-094: Log goal update
        auditService.logGoalChange(
                userId,
                userId,
                "UPDATE",
                oldGoalCopy,
                updatedGoal,
                getCurrentRequest());

        return mapToGoalResponse(updatedGoal);
    }

    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        UserGoal goal = goalRepository.findById(goalId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));

        // US-094: Log goal deletion (before deleting)
        auditService.logGoalChange(
                userId,
                userId,
                "DELETE",
                goal,
                null,
                getCurrentRequest());

        goalRepository.delete(goal);
    }

    // US-096: Mark goal as completed
    @Transactional
    public UserGoalResponse markGoalAsCompleted(UUID userId, UUID goalId) {
        UserGoal goal = goalRepository.findById(goalId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));

        UserGoal oldGoalCopy = copyGoal(goal);
        goal.markCompleted();
        UserGoal completedGoal = goalRepository.save(goal);

        // Audit logging
        auditService.logGoalChange(
                userId, userId, "COMPLETE",
                oldGoalCopy, completedGoal,
                getCurrentRequest());

        return mapToGoalResponse(completedGoal);
    }

    // US-096: Update goal progress
    @Transactional
    public UserGoalResponse updateGoalProgress(UUID userId, UUID goalId, int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }

        UserGoal goal = goalRepository.findById(goalId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));

        UserGoal oldGoalCopy = copyGoal(goal);
        goal.updateProgress(percentage);
        UserGoal updatedGoal = goalRepository.save(goal);

        // Audit logging
        auditService.logGoalChange(
                userId, userId, "UPDATE_PROGRESS",
                oldGoalCopy, updatedGoal,
                getCurrentRequest());

        return mapToGoalResponse(updatedGoal);
    }

    // US-096: Get goals by status
    public List<UserGoalResponse> getGoalsByStatus(UUID userId, String status) {
        return goalRepository.findByUserIdAndStatus(userId, status).stream()
                .map(this::mapToGoalResponse)
                .collect(Collectors.toList());
    }

    // US-096: Helper method to copy goal for audit trail
    private UserGoal copyGoal(UserGoal goal) {
        return UserGoal.builder()
                .id(goal.getId())
                .userId(goal.getUserId())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .domainId(goal.getDomainId())
                .skillId(goal.getSkillId()) // US-093
                .targetLevel(goal.getTargetLevel())
                .dueDate(goal.getDueDate())
                .intensity(goal.getIntensity())
                .isActive(goal.getIsActive())
                .completedAt(goal.getCompletedAt())
                .completionPercentage(goal.getCompletionPercentage())
                .status(goal.getStatus())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }

    // --- PREFERENCES ---

    @Transactional(readOnly = true)
    public UserStudyPreferencesResponse getPreferences(UUID userId) {
        return preferencesRepository.findById(userId)
                .map(this::mapToPreferencesResponse)
                .orElseGet(() -> UserStudyPreferencesResponse.builder()
                        .userId(userId)
                        .hoursPerWeek(5.0) // Defaults
                        .notificationsEnabled(true)
                        .build());
    }

    @Transactional
    public UserStudyPreferencesResponse updatePreferences(UUID userId, UserStudyPreferencesUpdate request) {
        UserStudyPreferences oldPrefs = preferencesRepository.findById(userId)
                .orElse(null);

        // Create copy for audit if exists
        UserStudyPreferences oldPrefsCopy = null;
        if (oldPrefs != null) {
            oldPrefsCopy = UserStudyPreferences.builder()
                    .userId(oldPrefs.getUserId())
                    .hoursPerWeek(oldPrefs.getHoursPerWeek())
                    .preferredDays(oldPrefs.getPreferredDays())
                    .preferredSessionMinutes(oldPrefs.getPreferredSessionMinutes())
                    .notificationsEnabled(oldPrefs.getNotificationsEnabled())
                    .build();
        }

        UserStudyPreferences prefs = oldPrefs != null ? oldPrefs
                : UserStudyPreferences.builder().userId(userId).build();

        if (request.getHoursPerWeek() != null)
            prefs.setHoursPerWeek(request.getHoursPerWeek());
        if (request.getPreferredDays() != null)
            prefs.setPreferredDays(request.getPreferredDays());
        if (request.getPreferredSessionMinutes() != null)
            prefs.setPreferredSessionMinutes(request.getPreferredSessionMinutes());
        if (request.getNotificationsEnabled() != null)
            prefs.setNotificationsEnabled(request.getNotificationsEnabled());

        UserStudyPreferences updatedPrefs = preferencesRepository.save(prefs);

        // US-094: Log preferences update/create
        String action = oldPrefs == null ? "CREATE" : "UPDATE";
        auditService.logPreferencesChange(
                userId,
                userId,
                action,
                oldPrefsCopy,
                updatedPrefs,
                getCurrentRequest());

        return mapToPreferencesResponse(updatedPrefs);
    }

    private UserProfileResponse mapToProfileResponse(UserProfile p) {
        return UserProfileResponse.builder()
                .userId(p.getUserId())
                .authUserId(p.getAuthUserId())
                .email(p.getEmail())
                .displayName(p.getDisplayName())
                .birthYear(p.getBirthYear())
                .locale(p.getLocale())
                .timezone(p.getTimezone())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private UserGoalResponse mapToGoalResponse(UserGoal g) {
        return UserGoalResponse.builder()
                .id(g.getId())
                .userId(g.getUserId())
                .title(g.getTitle())
                .description(g.getDescription())
                .domainId(g.getDomainId())
                .skillId(g.getSkillId()) // US-093
                .targetLevel(g.getTargetLevel())
                .dueDate(g.getDueDate())
                .intensity(g.getIntensity())
                .isActive(g.getIsActive())
                .completedAt(g.getCompletedAt()) // US-096
                .completionPercentage(g.getCompletionPercentage()) // US-096
                .status(g.getStatus()) // US-096
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    private UserStudyPreferencesResponse mapToPreferencesResponse(UserStudyPreferences p) {
        return UserStudyPreferencesResponse.builder()
                .userId(p.getUserId())
                .hoursPerWeek(p.getHoursPerWeek())
                .preferredDays(p.getPreferredDays())
                .preferredSessionMinutes(p.getPreferredSessionMinutes())
                .notificationsEnabled(p.getNotificationsEnabled())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserAuditLogResponse> getMyAuditLogs(UUID userId, int page, int size) {
        return auditService.getAuditTrail(userId, page, size).stream()
                .map(this::mapToAuditLogResponse)
                .collect(Collectors.toList());
    }

    private UserAuditLogResponse mapToAuditLogResponse(com.learnsmart.profile.model.UserAuditLog log) {
        return UserAuditLogResponse.builder()
                .id(log.getId())
                .performedBy(log.getPerformedBy())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .fieldName(log.getFieldName())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .timestamp(log.getTimestamp())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .build();
    }

    /**
     * Get current HTTP request from RequestContextHolder
     * Returns null if not in web context (e.g., during tests)
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null; // Not in web context
        }
    }
}
