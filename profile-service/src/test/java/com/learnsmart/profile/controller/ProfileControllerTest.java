package com.learnsmart.profile.controller;

import com.learnsmart.profile.dto.ProfileDtos.*;
import com.learnsmart.profile.service.ProfileServiceImpl;
import com.learnsmart.profile.service.ProgressService;
import com.learnsmart.profile.dto.ProgressDtos.UserProgressResponse;
import com.learnsmart.profile.dto.ProgressDtos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileServiceImpl profileService;

    @Mock
    private ProgressService progressService;

    @InjectMocks
    private ProfileController profileController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String authId) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(authId);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testCreateProfile() {
        UserRegistrationRequest request = UserRegistrationRequest.builder().email("test@test.com").build();
        UserProfileResponse responseDto = UserProfileResponse.builder().email("test@test.com").build();

        when(profileService.registerUser(request)).thenReturn(responseDto);

        ResponseEntity<UserProfileResponse> response = profileController.createProfile(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
    }

    @Test
    void testGetMyProfile_WithHeader() {
        UUID userId = UUID.randomUUID();
        String userIdStr = userId.toString();
        UserProfileResponse responseDto = UserProfileResponse.builder().userId(userId).build();

        when(profileService.getProfile(userId)).thenReturn(responseDto);

        ResponseEntity<UserProfileResponse> response = profileController.getMyProfile(userIdStr);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().getUserId());
    }

    @Test
    void testGetMyProfile_WithJwt() {
        String authId = "auth-123";
        UUID userId = UUID.randomUUID();
        mockSecurityContext(authId);

        UserProfileResponse profileByAuth = UserProfileResponse.builder().userId(userId).build();
        // The controller uses getProfileByAuthId to find the UUID, then
        // getProfile(UUID) ? No.
        // Controller calls: profileService.getProfileByAuthId(authUserId).getUserId()
        // Then calls: profileService.getProfile(thatUUID)

        when(profileService.getProfileByAuthId(authId)).thenReturn(profileByAuth);
        when(profileService.getProfile(userId)).thenReturn(profileByAuth);

        ResponseEntity<UserProfileResponse> response = profileController.getMyProfile(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().getUserId());
    }

    @Test
    void testGetMyProfile_NoAuth_NoHeader() {
        assertThrows(ResponseStatusException.class, () -> profileController.getMyProfile(null));
    }

    @Test
    void testUpdateMyProfile() {
        UUID userId = UUID.randomUUID();
        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder().displayName("New").build();
        UserProfileResponse responseDto = UserProfileResponse.builder().displayName("New").build();

        when(profileService.updateProfile(userId, request)).thenReturn(responseDto);

        ResponseEntity<UserProfileResponse> response = profileController.updateMyProfile(userId.toString(), request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("New", response.getBody().getDisplayName());
    }

    @Test
    void testGetUserProfile_ById() {
        UUID userId = UUID.randomUUID();
        UserProfileResponse responseDto = UserProfileResponse.builder().userId(userId).build();

        when(profileService.getProfile(userId)).thenReturn(responseDto);

        ResponseEntity<UserProfileResponse> response = profileController.getUserProfile(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().getUserId());
    }

    // --- Goals ---

    @Test
    void testGetMyGoals() {
        UUID userId = UUID.randomUUID();
        when(profileService.getUserGoals(userId)).thenReturn(List.of());

        ResponseEntity<List<UserGoalResponse>> response = profileController.getMyGoals(userId.toString());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testCreateGoal() {
        UUID userId = UUID.randomUUID();
        UserGoalCreateRequest request = UserGoalCreateRequest.builder().title("Goal").build();
        UserGoalResponse responseDto = UserGoalResponse.builder().title("Goal").build();

        when(profileService.createGoal(userId, request)).thenReturn(responseDto);

        ResponseEntity<UserGoalResponse> response = profileController.createGoal(userId.toString(), request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void testUpdateGoal() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoalUpdateRequest request = UserGoalUpdateRequest.builder().title("Updated").build();
        UserGoalResponse responseDto = UserGoalResponse.builder().title("Updated").build();

        when(profileService.updateGoal(userId, goalId, request)).thenReturn(responseDto);

        ResponseEntity<UserGoalResponse> response = profileController.updateGoal(userId.toString(), goalId, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteGoal() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();

        doNothing().when(profileService).deleteGoal(userId, goalId);

        ResponseEntity<Void> response = profileController.deleteGoal(userId.toString(), goalId);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    // --- Preferences ---

    @Test
    void testGetMyPreferences() {
        UUID userId = UUID.randomUUID();
        UserStudyPreferencesResponse responseDto = UserStudyPreferencesResponse.builder().build();

        when(profileService.getPreferences(userId)).thenReturn(responseDto);
        ResponseEntity<UserStudyPreferencesResponse> response = profileController.getMyPreferences(userId.toString());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetMyProgress() {
        UUID userId = UUID.randomUUID();
        UserProgressResponse mockProgress = UserProgressResponse.builder()
                .profile(ProgressDtos.ProfileInfo.builder().userId(userId.toString()).build())
                .build();

        when(progressService.getConsolidatedProgressByInternalId(userId)).thenReturn(mockProgress);

        ResponseEntity<UserProgressResponse> response = profileController.getMyProgress(userId.toString());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockProgress, response.getBody());
    }

    @Test
    void testUpdateMyPreferences() {
        UUID userId = UUID.randomUUID();
        UserStudyPreferencesUpdate request = UserStudyPreferencesUpdate.builder()
                .hoursPerWeek(10.0).build();
        UserStudyPreferencesResponse responseDto = UserStudyPreferencesResponse.builder()
                .hoursPerWeek(10.0).build();

        when(profileService.updatePreferences(userId, request)).thenReturn(responseDto);

        ResponseEntity<UserStudyPreferencesResponse> response = profileController.updateMyPreferences(userId.toString(),
                request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10.0, response.getBody().getHoursPerWeek());
    }

    @Test
    void testGetMyAuditLogs() {
        UUID userId = UUID.randomUUID();
        UserAuditLogResponse log = UserAuditLogResponse.builder()
                .action("CREATE").entityType("GOAL").build();
        when(profileService.getMyAuditLogs(userId, 0, 20)).thenReturn(List.of(log));

        ResponseEntity<List<UserAuditLogResponse>> response = profileController.getMyAuditLogs(userId.toString(), 0,
                20);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("CREATE", response.getBody().get(0).getAction());
    }

    @Test
    void testPatchGoal() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoalUpdateRequest request = UserGoalUpdateRequest.builder().status("COMPLETED").build();
        UserGoalResponse responseDto = UserGoalResponse.builder().status("COMPLETED").build();

        when(profileService.updateGoal(userId, goalId, request)).thenReturn(responseDto);

        ResponseEntity<UserGoalResponse> response = profileController.patchGoal(userId.toString(), goalId, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("COMPLETED", response.getBody().getStatus());
    }

    @Test
    void testMarkGoalAsCompleted() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoalResponse responseDto = UserGoalResponse.builder()
                .status("completed").completionPercentage(100).build();

        when(profileService.markGoalAsCompleted(userId, goalId)).thenReturn(responseDto);

        ResponseEntity<UserGoalResponse> response = profileController.markGoalAsCompleted(userId.toString(), goalId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("completed", response.getBody().getStatus());
        assertEquals(100, response.getBody().getCompletionPercentage());
    }

    @Test
    void testUpdateGoalProgress() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoalResponse responseDto = UserGoalResponse.builder()
                .completionPercentage(75).status("in_progress").build();

        when(profileService.updateGoalProgress(userId, goalId, 75)).thenReturn(responseDto);

        ResponseEntity<UserGoalResponse> response = profileController.updateGoalProgress(userId.toString(), goalId,
                java.util.Map.of("percentage", 75));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(75, response.getBody().getCompletionPercentage());
    }

    @Test
    void testGetGoalsByStatus() {
        UUID userId = UUID.randomUUID();
        UserGoalResponse goal = UserGoalResponse.builder().status("completed").title("Done").build();
        when(profileService.getGoalsByStatus(userId, "completed")).thenReturn(List.of(goal));

        ResponseEntity<List<UserGoalResponse>> response = profileController.getGoalsByStatus(userId.toString(),
                "completed");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Done", response.getBody().get(0).getTitle());
    }

    @Test
    void testGetMyProgress_ViaJwt() {
        String authId = "auth-jwt-123";
        UUID userId = UUID.randomUUID();
        mockSecurityContext(authId);

        UserProfileResponse profileByAuth = UserProfileResponse.builder().userId(userId).build();
        when(profileService.getProfileByAuthId(authId)).thenReturn(profileByAuth);

        com.learnsmart.profile.dto.ProgressDtos.UserProgressResponse mockProgress = com.learnsmart.profile.dto.ProgressDtos.UserProgressResponse
                .builder()
                .profile(com.learnsmart.profile.dto.ProgressDtos.ProfileInfo.builder()
                        .userId(userId.toString()).build())
                .build();
        when(progressService.getConsolidatedProgressByInternalId(userId)).thenReturn(mockProgress);

        ResponseEntity<com.learnsmart.profile.dto.ProgressDtos.UserProgressResponse> response = profileController
                .getMyProgress(null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(progressService).getConsolidatedProgressByInternalId(userId);
    }

    @Test
    void testUpdateGoalProgress_MissingPercentageKey_DefaultsToZero() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoalResponse responseDto = UserGoalResponse.builder().completionPercentage(0).build();

        when(profileService.updateGoalProgress(userId, goalId, 0)).thenReturn(responseDto);

        // Empty map → percentage defaults to 0
        ResponseEntity<UserGoalResponse> response = profileController.updateGoalProgress(userId.toString(), goalId,
                java.util.Map.of());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getCompletionPercentage());
    }
}
