package com.learnsmart.profile.controller;

import com.learnsmart.profile.dto.ProfileDtos.*;
import com.learnsmart.profile.service.ProfileServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileServiceImpl profileService;
    private final com.learnsmart.profile.service.ProgressService progressService;

    private UUID getUserId(String xUserId) {
        // 1. Fallback to Header (Legacy/Testing/Override) - Takes precedence for
        // integration tests
        if (xUserId != null && !xUserId.isBlank() && !xUserId.startsWith("{{")) {
            try {
                return UUID.fromString(xUserId);
            } catch (Exception e) {
                // Ignore and try JWT
            }
        }

        // 2. Try to get from Security Context (JWT) - subject is Keycloak auth ID
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            String authUserId = jwt.getSubject();
            if (authUserId != null) {
                // Look up profile by authUserId (Keycloak ID)
                try {
                    return profileService.getProfileByAuthId(authUserId).getUserId();
                } catch (Exception e) {
                    // If no profile yet for this auth user, we must rely on registration or header
                }
            }
        }

        if (xUserId == null || xUserId.isBlank() || xUserId.startsWith("{{")) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User ID not found in Token or Header");
        }

        return UUID.fromString(xUserId);
    }

    private String getAuthId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }

    @GetMapping("/me/progress")
    public ResponseEntity<com.learnsmart.profile.dto.ProgressDtos.UserProgressResponse> getMyProgress(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId) {
        return ResponseEntity.ok(progressService.getConsolidatedProgressByInternalId(getUserId(xUserId)));
    }

    // US-094: Audit trail endpoint
    @GetMapping("/me/audit-logs")
    public ResponseEntity<List<UserAuditLogResponse>> getMyAuditLogs(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(profileService.getMyAuditLogs(getUserId(xUserId), page, size));
    }

    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(@RequestBody @Valid UserRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.registerUser(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId) {
        // En un entorno real, el Gateway DEBE mandar este header.
        // Para pruebas locales, se puede pasar manualmente.
        return ResponseEntity.ok(profileService.getProfile(getUserId(xUserId)));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(getUserId(xUserId), request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    // --- GOALS ---

    @GetMapping("/me/goals")
    public ResponseEntity<List<UserGoalResponse>> getMyGoals(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId) {
        return ResponseEntity.ok(profileService.getUserGoals(getUserId(xUserId)));
    }

    @PostMapping("/me/goals")
    public ResponseEntity<UserGoalResponse> createGoal(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestBody @Valid UserGoalCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.createGoal(getUserId(xUserId), request));
    }

    @PutMapping("/me/goals/{goalId}")
    public ResponseEntity<UserGoalResponse> updateGoal(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID goalId,
            @RequestBody UserGoalUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateGoal(getUserId(xUserId), goalId, request));
    }

    // US-096: PATCH for partial goal updates (status, progress, etc.)
    @PatchMapping("/me/goals/{goalId}")
    public ResponseEntity<UserGoalResponse> patchGoal(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID goalId,
            @RequestBody UserGoalUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateGoal(getUserId(xUserId), goalId, request));
    }

    @DeleteMapping("/me/goals/{goalId}")
    public ResponseEntity<Void> deleteGoal(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID goalId) {
        profileService.deleteGoal(getUserId(xUserId), goalId);
        return ResponseEntity.noContent().build();
    }

    // US-096: Mark goal as completed
    @PostMapping("/me/goals/{goalId}/complete")
    public ResponseEntity<UserGoalResponse> markGoalAsCompleted(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID goalId) {
        return ResponseEntity.ok(profileService.markGoalAsCompleted(getUserId(xUserId), goalId));
    }

    // US-096: Update goal progress
    @PatchMapping("/me/goals/{goalId}/progress")
    public ResponseEntity<UserGoalResponse> updateGoalProgress(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID goalId,
            @RequestBody java.util.Map<String, Integer> request) {
        int percentage = request.getOrDefault("percentage", 0);
        return ResponseEntity.ok(profileService.updateGoalProgress(getUserId(xUserId), goalId, percentage));
    }

    // US-096: Get goals by status
    @GetMapping("/me/goals/status/{status}")
    public ResponseEntity<List<UserGoalResponse>> getGoalsByStatus(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable String status) {
        return ResponseEntity.ok(profileService.getGoalsByStatus(getUserId(xUserId), status));
    }

    // --- PREFERENCES ---

    @GetMapping("/me/preferences")
    public ResponseEntity<UserStudyPreferencesResponse> getMyPreferences(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId) {
        return ResponseEntity.ok(profileService.getPreferences(getUserId(xUserId)));
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<UserStudyPreferencesResponse> updateMyPreferences(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestBody UserStudyPreferencesUpdate request) {
        return ResponseEntity.ok(profileService.updatePreferences(getUserId(xUserId), request));
    }
}
