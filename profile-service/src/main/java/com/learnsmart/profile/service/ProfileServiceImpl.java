package com.learnsmart.profile.service;

import com.learnsmart.profile.dto.ProfileDtos.*;
import com.learnsmart.profile.model.UserGoal;
import com.learnsmart.profile.model.UserProfile;
import com.learnsmart.profile.repository.UserGoalRepository;
import com.learnsmart.profile.repository.UserProfileRepository;
import com.learnsmart.profile.repository.UserStudyPreferencesRepository;
import com.learnsmart.profile.model.UserStudyPreferences;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl {

    private final UserProfileRepository profileRepository;
    private final UserGoalRepository goalRepository;
    private final UserStudyPreferencesRepository preferencesRepository;

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
        return profileRepository.findByAuthUserId(authUserId)
                .map(this::mapToProfileResponse)
                .orElseThrow(() -> new IllegalArgumentException("User not found for authUserId: " + authUserId));
    }

    // Método auxiliar para buscar por ID de usuario interno (simulo "me" si tuviera
    // contexto de seguridad)
    // Para esta prueba, asumiremos que el controller extrae el ID correcto.

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UserProfileUpdateRequest request) {
        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getDisplayName() != null)
            profile.setDisplayName(request.getDisplayName());
        if (request.getBirthYear() != null)
            profile.setBirthYear(request.getBirthYear());
        if (request.getLocale() != null)
            profile.setLocale(request.getLocale());
        if (request.getTimezone() != null)
            profile.setTimezone(request.getTimezone());

        profile = profileRepository.save(profile);
        return mapToProfileResponse(profile);
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
                .domain(request.getDomain())
                .targetLevel(request.getTargetLevel())
                .dueDate(request.getDueDate())
                .intensity(request.getIntensity())
                .isActive(true)
                .build();

        goal = goalRepository.save(goal);
        return mapToGoalResponse(goal);
    }

    @Transactional
    public UserGoalResponse updateGoal(UUID userId, UUID goalId, UserGoalUpdateRequest request) {
        UserGoal goal = goalRepository.findById(goalId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));

        if (request.getTitle() != null)
            goal.setTitle(request.getTitle());
        if (request.getDescription() != null)
            goal.setDescription(request.getDescription());
        if (request.getDomain() != null)
            goal.setDomain(request.getDomain());
        if (request.getTargetLevel() != null)
            goal.setTargetLevel(request.getTargetLevel());
        if (request.getDueDate() != null)
            goal.setDueDate(request.getDueDate());
        if (request.getIntensity() != null)
            goal.setIntensity(request.getIntensity());
        if (request.getIsActive() != null)
            goal.setIsActive(request.getIsActive());

        goal = goalRepository.save(goal);
        return mapToGoalResponse(goal);
    }

    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        UserGoal goal = goalRepository.findById(goalId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        goalRepository.delete(goal);
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
        UserStudyPreferences prefs = preferencesRepository.findById(userId)
                .orElse(UserStudyPreferences.builder().userId(userId).build());

        if (request.getHoursPerWeek() != null)
            prefs.setHoursPerWeek(request.getHoursPerWeek());
        if (request.getPreferredDays() != null)
            prefs.setPreferredDays(request.getPreferredDays());
        if (request.getPreferredSessionMinutes() != null)
            prefs.setPreferredSessionMinutes(request.getPreferredSessionMinutes());
        if (request.getNotificationsEnabled() != null)
            prefs.setNotificationsEnabled(request.getNotificationsEnabled());

        prefs = preferencesRepository.save(prefs);
        return mapToPreferencesResponse(prefs);
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
                .domain(g.getDomain())
                .targetLevel(g.getTargetLevel())
                .dueDate(g.getDueDate())
                .intensity(g.getIntensity())
                .isActive(g.getIsActive())
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

    public UserProfileResponse findByEmail(String email) {
        UserProfile profile = profileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        
        return mapToProfileResponse(profile);
    }
}
