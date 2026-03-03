package com.learnsmart.profile.service;

import com.learnsmart.profile.model.UserAuditLog;
import com.learnsmart.profile.model.UserGoal;
import com.learnsmart.profile.model.UserProfile;
import com.learnsmart.profile.model.UserStudyPreferences;
import com.learnsmart.profile.repository.UserAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private UserAuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    // -------------------------------------------------------------------------
    // logProfileChange
    // -------------------------------------------------------------------------

    @Test
    void testLogProfileChange_SavesLogWithCorrectEntityType() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        HttpServletRequest request = mock(HttpServletRequest.class);
        // Default all getHeader() calls to null so strict-stubbing doesn't trip
        // on intermediate headers (X-Real-IP, etc.) the extractor also reads.
        when(request.getHeader(anyString())).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        ArgumentCaptor<UserAuditLog> captor = ArgumentCaptor.forClass(UserAuditLog.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        auditService.logProfileChange(userId, userId, "CREATE", null, profile, request);

        // @Async runs synchronously in unit tests (no TaskExecutor configured)
        UserAuditLog saved = captor.getValue();
        assertEquals("PROFILE", saved.getEntityType());
        assertEquals("CREATE", saved.getAction());
        assertEquals(userId, saved.getUserId());
        assertNotNull(saved.getNewValue(), "New value must be serialized");
        assertNull(saved.getOldValue(), "Old value must be null for CREATE");
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertEquals("JUnit", saved.getUserAgent());
    }

    @Test
    void testLogProfileChange_NullRequest_DoesNotThrow() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> auditService.logProfileChange(userId, userId, "UPDATE", null, profile, null));
    }

    // -------------------------------------------------------------------------
    // logGoalChange — entityId derived from old/new value
    // -------------------------------------------------------------------------

    @Test
    void testLogGoalChange_EntityIdFromNewValue_WhenOldIsNull() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoal newGoal = UserGoal.builder().id(goalId).userId(userId).title("New").build();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(anyString())).thenReturn(null); // covers X-Forwarded-For, X-Real-IP, User-Agent
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        ArgumentCaptor<UserAuditLog> captor = ArgumentCaptor.forClass(UserAuditLog.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        auditService.logGoalChange(userId, userId, "CREATE", null, newGoal, request);

        assertEquals("GOAL", captor.getValue().getEntityType());
        assertEquals(goalId, captor.getValue().getEntityId());
    }

    @Test
    void testLogGoalChange_EntityIdFromOldValue_WhenNewIsNull() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UserGoal oldGoal = UserGoal.builder().id(goalId).userId(userId).title("Old").build();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(anyString())).thenReturn(null); // covers all intermediate headers
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        ArgumentCaptor<UserAuditLog> captor = ArgumentCaptor.forClass(UserAuditLog.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        auditService.logGoalChange(userId, userId, "DELETE", oldGoal, null, request);

        assertEquals(goalId, captor.getValue().getEntityId());
        assertNull(captor.getValue().getNewValue());
    }

    // -------------------------------------------------------------------------
    // logPreferencesChange
    // -------------------------------------------------------------------------

    @Test
    void testLogPreferencesChange_SavesWithCorrectEntityType() {
        UUID userId = UUID.randomUUID();
        UserStudyPreferences prefs = UserStudyPreferences.builder().userId(userId).hoursPerWeek(5.0).build();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(anyString())).thenReturn(null); // covers all intermediate headers
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        ArgumentCaptor<UserAuditLog> captor = ArgumentCaptor.forClass(UserAuditLog.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        auditService.logPreferencesChange(userId, userId, "UPDATE", null, prefs, request);

        assertEquals("PREFERENCES", captor.getValue().getEntityType());
        assertEquals(userId, captor.getValue().getEntityId(), "For preferences entityId equals userId");
    }

    // -------------------------------------------------------------------------
    // extractIpAddress — X-Forwarded-For branch
    // -------------------------------------------------------------------------

    @Test
    void testLogProfileChange_XForwardedFor_TakesFirstIp() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 198.51.100.1");

        ArgumentCaptor<UserAuditLog> captor = ArgumentCaptor.forClass(UserAuditLog.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        auditService.logProfileChange(userId, userId, "UPDATE", profile, profile, request);

        assertEquals("203.0.113.5", captor.getValue().getIpAddress(),
                "Only the first IP in X-Forwarded-For chain must be stored");
    }

    // -------------------------------------------------------------------------
    // getAuditTrail / getEntityAuditTrail / getAuditTrailByDateRange
    // -------------------------------------------------------------------------

    @Test
    void testGetAuditTrail_DelegatesToRepository() {
        UUID userId = UUID.randomUUID();
        UserAuditLog log = new UserAuditLog();
        when(auditLogRepository.findByUserIdOrderByTimestampDesc(any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of(log));

        List<UserAuditLog> result = auditService.getAuditTrail(userId, 0, 10);

        assertEquals(1, result.size());
        verify(auditLogRepository).findByUserIdOrderByTimestampDesc(eq(userId), any(Pageable.class));
    }

    @Test
    void testGetEntityAuditTrail_DelegatesToRepository() {
        UUID entityId = UUID.randomUUID();
        when(auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
                any(), any(), any(Pageable.class))).thenReturn(List.of());

        List<UserAuditLog> result = auditService.getEntityAuditTrail("GOAL", entityId, 0, 5);

        assertTrue(result.isEmpty());
        verify(auditLogRepository).findByEntityTypeAndEntityIdOrderByTimestampDesc(
                eq("GOAL"), eq(entityId), any(Pageable.class));
    }

    @Test
    void testGetAuditTrailByDateRange_DelegatesToRepository() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().minusDays(7);
        OffsetDateTime to = OffsetDateTime.now();
        when(auditLogRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(userId, from, to))
                .thenReturn(List.of());

        List<UserAuditLog> result = auditService.getAuditTrailByDateRange(userId, from, to);

        assertTrue(result.isEmpty());
        verify(auditLogRepository).findByUserIdAndTimestampBetweenOrderByTimestampDesc(userId, from, to);
    }
}
