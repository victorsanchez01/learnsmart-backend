package com.learnsmart.profile.service;

import com.learnsmart.profile.dto.ProfileDtos.*;
import com.learnsmart.profile.model.UserGoal;
import com.learnsmart.profile.model.UserProfile;
import com.learnsmart.profile.model.UserStudyPreferences;
import com.learnsmart.profile.repository.UserGoalRepository;
import com.learnsmart.profile.repository.UserProfileRepository;
import com.learnsmart.profile.repository.UserStudyPreferencesRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    /**
     * Default hours/week returned when no preferences record exists (mirrors
     * service impl).
     */
    private static final double DEFAULT_HOURS_PER_WEEK = 5.0;

    @Mock
    private UserProfileRepository profileRepository;
    @Mock
    private UserGoalRepository goalRepository;
    @Mock
    private UserStudyPreferencesRepository preferencesRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private com.learnsmart.profile.client.ContentServiceClient contentClient;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // registerUser
    // -------------------------------------------------------------------------

    @Test
    void testRegisterUser_Success_WithJwt() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth-uuid-123");
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("test@example.com")
                .displayName("Test User")
                .build();

        when(profileRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = profileService.registerUser(request);

        assertNotNull(response);
        assertEquals("auth-uuid-123", response.getAuthUserId());
        assertEquals("test@example.com", response.getEmail());
        verify(profileRepository).save(any(UserProfile.class));
    }

    @Test
    void testRegisterUser_NoJwt_FallsBackToGeneratedUuid() {
        // Behavior: without a JWT the service generates a random UUID for authUserId.
        // This is documented as a fallback for testing/legacy scenarios.
        SecurityContextHolder.clearContext();

        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("test@example.com")
                .displayName("Test User")
                .build();

        when(profileRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = profileService.registerUser(request);

        assertNotNull(response);
        assertNotNull(response.getAuthUserId(), "A UUID must be assigned even without JWT");
        verify(profileRepository).save(any(UserProfile.class));
    }

    @Test
    void testRegisterUser_EmailAlreadyExists_Throws() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("existing@example.com")
                .build();

        when(profileRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new UserProfile()));

        assertThrows(IllegalArgumentException.class, () -> profileService.registerUser(request));
        verify(profileRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getProfile
    // -------------------------------------------------------------------------

    @Test
    void testGetProfile_Found() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder().userId(userId).email("test@example.com").build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        UserProfileResponse response = profileService.getProfile(userId);
        assertEquals(userId, response.getUserId());
    }

    @Test
    void testGetProfile_NotFound() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> profileService.getProfile(userId));
    }

    // -------------------------------------------------------------------------
    // getProfileByAuthId
    // -------------------------------------------------------------------------

    @Test
    void testGetProfileByAuthId_Found() {
        String authId = "auth-123";
        UserProfile profile = UserProfile.builder().authUserId(authId).build();
        when(profileRepository.findFirstByAuthUserId(authId)).thenReturn(Optional.of(profile));

        UserProfileResponse response = profileService.getProfileByAuthId(authId);
        assertEquals(authId, response.getAuthUserId());
    }

    @Test
    void testGetProfileByAuthId_NotFound() {
        String authId = "auth-123";
        when(profileRepository.findFirstByAuthUserId(authId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> profileService.getProfileByAuthId(authId));
    }

    // -------------------------------------------------------------------------
    // updateProfile
    // -------------------------------------------------------------------------

    @Test
    void testUpdateProfile_AllFields() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder().userId(userId).build();

        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
                .displayName("New Name")
                .birthYear(1990)
                .locale("en-US")
                .timezone("UTC")
                .build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse response = profileService.updateProfile(userId, request);

        assertEquals("New Name", response.getDisplayName());
        assertEquals(1990, response.getBirthYear());
        assertEquals("en-US", response.getLocale());
        assertEquals("UTC", response.getTimezone());
    }

    @Test
    void testUpdateProfile_PartialFields_DoesNotClearUnsetFields() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .displayName("Original")
                .locale("es-ES")
                .build();

        // Only updating displayName; locale must remain unchanged
        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
                .displayName("Updated")
                .build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse response = profileService.updateProfile(userId, request);

        assertEquals("Updated", response.getDisplayName());
        assertEquals("es-ES", response.getLocale(), "Unset fields must not be overwritten");
    }

    @Test
    void testUpdateProfile_NotFound() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> profileService.updateProfile(userId, UserProfileUpdateRequest.builder().build()));
    }

    // -------------------------------------------------------------------------
    // Goals
    // -------------------------------------------------------------------------

    @Test
    void testCreateGoal() {
        UUID userId = UUID.randomUUID();
        UserGoalCreateRequest request = UserGoalCreateRequest.builder()
                .title("Learn Java")
                .build();

        when(goalRepository.save(any(UserGoal.class))).thenAnswer(i -> {
            UserGoal g = i.getArgument(0);
            g.setId(UUID.randomUUID());
            return g;
        });

        UserGoalResponse response = profileService.createGoal(userId, request);

        assertNotNull(response.getId());
        assertEquals("Learn Java", response.getTitle());
        assertTrue(response.getIsActive());
    }

    @Test
    void testGetUserGoals() {
        UUID userId = UUID.randomUUID();
        UserGoal goal = UserGoal.builder().userId(userId).title("Goal 1").build();
        when(goalRepository.findByUserId(userId)).thenReturn(List.of(goal));

        List<UserGoalResponse> responses = profileService.getUserGoals(userId);
        assertEquals(1, responses.size());
        assertEquals("Goal 1", responses.get(0).getTitle());
    }

    @Test
    void testUpdateGoal_Success() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoal goal = UserGoal.builder().id(goalId).userId(userId).title("Old").build();

        UserGoalUpdateRequest request = UserGoalUpdateRequest.builder()
                .title("New")
                .description("Desc")
                .domainId(UUID.randomUUID())
                .targetLevel("HIGH")
                .dueDate(LocalDate.now())
                .intensity("HIGH")
                .isActive(false)
                .build();

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(contentClient.getDomain(any()))
                .thenReturn(new com.learnsmart.profile.client.ContentServiceClient.DomainDto());
        when(goalRepository.save(any(UserGoal.class))).thenAnswer(i -> i.getArgument(0));

        UserGoalResponse response = profileService.updateGoal(userId, goalId, request);

        assertEquals("New", response.getTitle());
        assertEquals("Desc", response.getDescription());
        assertFalse(response.getIsActive());
    }

    @Test
    void testUpdateGoal_NotFound() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalRepository.findById(goalId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> profileService.updateGoal(userId, goalId, UserGoalUpdateRequest.builder().build()));
    }

    @Test
    void testUpdateGoal_WrongUser() {
        UUID userId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoal goal = UserGoal.builder().id(goalId).userId(otherUser).build();

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));

        assertThrows(IllegalArgumentException.class,
                () -> profileService.updateGoal(userId, goalId, UserGoalUpdateRequest.builder().build()));
    }

    @Test
    void testDeleteGoal_Success() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoal goal = UserGoal.builder().id(goalId).userId(userId).build();

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));

        profileService.deleteGoal(userId, goalId);

        verify(goalRepository).delete(goal);
    }

    @Test
    void testDeleteGoal_NotFound() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalRepository.findById(goalId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> profileService.deleteGoal(userId, goalId));
    }

    @Test
    void testDeleteGoal_WrongUser_Throws() {
        // Cross-user authorization boundary: a user must not be able to delete another
        // user's goal.
        UUID userId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoal goal = UserGoal.builder().id(goalId).userId(otherUser).build();

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));

        assertThrows(IllegalArgumentException.class,
                () -> profileService.deleteGoal(userId, goalId),
                "Deleting another user's goal must throw");
        verify(goalRepository, never()).delete(any());
    }

    // -------------------------------------------------------------------------
    // Preferences
    // -------------------------------------------------------------------------

    @Test
    void testGetPreferences_Found() {
        UUID userId = UUID.randomUUID();
        UserStudyPreferences prefs = UserStudyPreferences.builder()
                .userId(userId)
                .hoursPerWeek(10.0)
                .build();
        when(preferencesRepository.findById(userId)).thenReturn(Optional.of(prefs));

        UserStudyPreferencesResponse response = profileService.getPreferences(userId);
        assertEquals(10.0, response.getHoursPerWeek());
    }

    @Test
    void testGetPreferences_NotFound_ReturnsDefaults() {
        UUID userId = UUID.randomUUID();
        when(preferencesRepository.findById(userId)).thenReturn(Optional.empty());

        UserStudyPreferencesResponse response = profileService.getPreferences(userId);
        assertEquals(DEFAULT_HOURS_PER_WEEK, response.getHoursPerWeek(),
                "Default hoursPerWeek must be " + DEFAULT_HOURS_PER_WEEK);
        assertTrue(response.getNotificationsEnabled(), "Notifications must default to enabled");
    }

    @Test
    void testUpdatePreferences_CreateNew() {
        UUID userId = UUID.randomUUID();
        UserStudyPreferencesUpdate request = UserStudyPreferencesUpdate.builder()
                .hoursPerWeek(8.0)
                .notificationsEnabled(true)
                .build();

        when(preferencesRepository.findById(userId)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any(UserStudyPreferences.class))).thenAnswer(i -> i.getArgument(0));

        UserStudyPreferencesResponse response = profileService.updatePreferences(userId, request);

        assertEquals(8.0, response.getHoursPerWeek());
        assertTrue(response.getNotificationsEnabled());
    }

    @Test
    void testUpdatePreferences_UpdateExisting() {
        UUID userId = UUID.randomUUID();
        UserStudyPreferences prefs = UserStudyPreferences.builder().userId(userId).hoursPerWeek(2.0).build();

        UserStudyPreferencesUpdate request = UserStudyPreferencesUpdate.builder()
                .hoursPerWeek(20.0)
                .preferredDays(List.of("MONDAY"))
                .preferredSessionMinutes(45)
                .notificationsEnabled(false)
                .build();

        when(preferencesRepository.findById(userId)).thenReturn(Optional.of(prefs));
        when(preferencesRepository.save(any(UserStudyPreferences.class))).thenAnswer(i -> i.getArgument(0));

        UserStudyPreferencesResponse response = profileService.updatePreferences(userId, request);

        assertEquals(20.0, response.getHoursPerWeek());
        assertEquals(45, response.getPreferredSessionMinutes());
        assertFalse(response.getNotificationsEnabled());
        assertEquals(1, response.getPreferredDays().size());
    }

    // -------------------------------------------------------------------------
    // createGoal — validation branches (domainId / skillId)
    // -------------------------------------------------------------------------

    @Test
    void testCreateGoal_WithDomainId_Validates() {
        UUID userId = UUID.randomUUID();
        UUID domainId = UUID.randomUUID();
        UserGoalCreateRequest request = UserGoalCreateRequest.builder()
                .title("Goal with domain")
                .domainId(domainId)
                .build();

        when(contentClient.getDomain(domainId))
                .thenReturn(new com.learnsmart.profile.client.ContentServiceClient.DomainDto());
        when(goalRepository.save(any(UserGoal.class))).thenAnswer(i -> i.getArgument(0));

        UserGoalResponse response = profileService.createGoal(userId, request);
        assertNotNull(response);
        verify(contentClient).getDomain(domainId);
    }

    @Test
    void testCreateGoal_DomainNotFound_Throws() {
        UUID userId = UUID.randomUUID();
        UUID domainId = UUID.randomUUID();
        UserGoalCreateRequest request = UserGoalCreateRequest.builder()
                .title("Goal")
                .domainId(domainId)
                .build();

        when(contentClient.getDomain(domainId))
                .thenThrow(new RuntimeException("Domain service down"));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> profileService.createGoal(userId, request));
        verify(goalRepository, never()).save(any());
    }

    @Test
    void testCreateGoal_SkillBelongsToWrongDomain_Throws() {
        UUID userId = UUID.randomUUID();
        UUID domainId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        UUID otherDomainId = UUID.randomUUID(); // Different from domainId

        UserGoalCreateRequest request = UserGoalCreateRequest.builder()
                .title("Goal")
                .domainId(domainId)
                .skillId(skillId)
                .build();

        com.learnsmart.profile.client.ContentServiceClient.DomainDto domain = new com.learnsmart.profile.client.ContentServiceClient.DomainDto();
        when(contentClient.getDomain(domainId)).thenReturn(domain);

        com.learnsmart.profile.client.ContentServiceClient.SkillDto skill = new com.learnsmart.profile.client.ContentServiceClient.SkillDto();
        com.learnsmart.profile.client.ContentServiceClient.DomainDto otherDomain = new com.learnsmart.profile.client.ContentServiceClient.DomainDto();
        otherDomain.setId(otherDomainId);
        skill.setDomain(otherDomain);
        skill.setCode("SKILL_CODE");
        when(contentClient.getSkill(skillId)).thenReturn(skill);

        // Skill domain doesn't match goal domain → service throws 400
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> profileService.createGoal(userId, request));
        verify(goalRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // markGoalAsCompleted
    // -------------------------------------------------------------------------

    @Test
    void testMarkGoalAsCompleted_Success() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoal goal = UserGoal.builder()
                .id(goalId)
                .userId(userId)
                .title("Complete me")
                .completionPercentage(80)
                .status("in_progress")
                .build();

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(UserGoal.class))).thenAnswer(i -> i.getArgument(0));

        UserGoalResponse response = profileService.markGoalAsCompleted(userId, goalId);

        assertEquals("completed", response.getStatus());
        assertEquals(100, response.getCompletionPercentage());
        assertNotNull(response.getCompletedAt());
    }

    @Test
    void testMarkGoalAsCompleted_WrongUser_Throws() {
        UUID userId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoal goal = UserGoal.builder().id(goalId).userId(otherUser).build();

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));

        assertThrows(IllegalArgumentException.class,
                () -> profileService.markGoalAsCompleted(userId, goalId));
        verify(goalRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // updateGoalProgress
    // -------------------------------------------------------------------------

    @Test
    void testUpdateGoalProgress_Partial_SetsInProgress() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoal goal = UserGoal.builder()
                .id(goalId)
                .userId(userId)
                .completionPercentage(0)
                .status("active")
                .build();

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(UserGoal.class))).thenAnswer(i -> i.getArgument(0));

        UserGoalResponse response = profileService.updateGoalProgress(userId, goalId, 50);

        assertEquals(50, response.getCompletionPercentage());
        assertEquals("in_progress", response.getStatus());
    }

    @Test
    void testUpdateGoalProgress_100_AutoCompletes() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoal goal = UserGoal.builder()
                .id(goalId)
                .userId(userId)
                .completionPercentage(80)
                .status("in_progress")
                .build();

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(UserGoal.class))).thenAnswer(i -> i.getArgument(0));

        UserGoalResponse response = profileService.updateGoalProgress(userId, goalId, 100);

        assertEquals(100, response.getCompletionPercentage());
        assertEquals("completed", response.getStatus());
        assertNotNull(response.getCompletedAt());
    }

    @Test
    void testUpdateGoalProgress_Invalid_Throws() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> profileService.updateGoalProgress(userId, goalId, 150),
                "Percentage > 100 must throw");

        assertThrows(IllegalArgumentException.class,
                () -> profileService.updateGoalProgress(userId, goalId, -1),
                "Percentage < 0 must throw");

        verify(goalRepository, never()).findById(any());
    }

    // -------------------------------------------------------------------------
    // getGoalsByStatus
    // -------------------------------------------------------------------------

    @Test
    void testGetGoalsByStatus() {
        UUID userId = UUID.randomUUID();
        UserGoal goal = UserGoal.builder().userId(userId).status("completed").title("Done").build();
        when(goalRepository.findByUserIdAndStatus(userId, "completed")).thenReturn(List.of(goal));

        List<UserGoalResponse> responses = profileService.getGoalsByStatus(userId, "completed");

        assertEquals(1, responses.size());
        assertEquals("Done", responses.get(0).getTitle());
        verify(goalRepository).findByUserIdAndStatus(userId, "completed");
    }

    // -------------------------------------------------------------------------
    // getMyAuditLogs
    // -------------------------------------------------------------------------

    @Test
    void testGetMyAuditLogs_ReturnsMappedLogs() {
        UUID userId = UUID.randomUUID();
        com.learnsmart.profile.model.UserAuditLog log = new com.learnsmart.profile.model.UserAuditLog();
        log.setId(UUID.randomUUID());
        log.setAction("CREATE");
        log.setEntityType("PROFILE");

        when(auditService.getAuditTrail(userId, 0, 10)).thenReturn(List.of(log));

        List<UserAuditLogResponse> responses = profileService.getMyAuditLogs(userId, 0, 10);

        assertEquals(1, responses.size());
        assertEquals("CREATE", responses.get(0).getAction());
        assertEquals("PROFILE", responses.get(0).getEntityType());
        verify(auditService).getAuditTrail(userId, 0, 10);
    }

    // -------------------------------------------------------------------------
    // updateGoal — auto-complete on status COMPLETED
    // -------------------------------------------------------------------------

    @Test
    void testUpdateGoal_StatusCompleted_SetsCompletedAt() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoal goal = UserGoal.builder()
                .id(goalId)
                .userId(userId)
                .title("Goal")
                .completedAt(null)
                .build();

        UserGoalUpdateRequest request = UserGoalUpdateRequest.builder()
                .status("COMPLETED")
                .build();

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(UserGoal.class))).thenAnswer(i -> i.getArgument(0));

        UserGoalResponse response = profileService.updateGoal(userId, goalId, request);

        assertNotNull(response.getCompletedAt(), "completedAt must be set when status is COMPLETED");
        assertEquals("COMPLETED", response.getStatus());
    }

    @Test
    void testUpdateGoal_Progress100_AutoCompletes() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoal goal = UserGoal.builder()
                .id(goalId)
                .userId(userId)
                .title("Goal")
                .completionPercentage(80)
                .completedAt(null)
                .build();

        UserGoalUpdateRequest request = UserGoalUpdateRequest.builder()
                .progressPercentage(100)
                .build();

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(UserGoal.class))).thenAnswer(i -> i.getArgument(0));

        UserGoalResponse response = profileService.updateGoal(userId, goalId, request);

        assertEquals(100, response.getCompletionPercentage());
        assertEquals("COMPLETED", response.getStatus());
        assertNotNull(response.getCompletedAt());
    }
}
