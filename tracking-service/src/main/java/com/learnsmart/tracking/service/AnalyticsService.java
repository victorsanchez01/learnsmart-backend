package com.learnsmart.tracking.service;

import com.learnsmart.tracking.dto.DailyActivityResponse;
import com.learnsmart.tracking.dto.UserStatsResponse;
import com.learnsmart.tracking.model.LearningEvent;
import com.learnsmart.tracking.repository.LearningEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final LearningEventRepository eventRepository;

    public UserStatsResponse calculateStats(UUID userId) {
        // Get all events for the user (paginated to avoid memory issues)
        var events = eventRepository.findEvents(
                userId, null, null, null, null, null,
                PageRequest.of(0, 10000)).getContent();

        // Calculate total hours from duration in payloads
        double totalHours = events.stream()
                .mapToDouble(this::extractDurationSeconds)
                .sum() / 3600.0;

        // Calculate current streak
        int streak = calculateStreak(events);

        // Count unique lessons completed
        long lessonsCompleted = events.stream()
                .filter(e -> "content_view".equals(e.getEventType()))
                .map(LearningEvent::getEntityId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // Count assessments taken
        long assessmentsTaken = events.stream()
                .filter(e -> "assessment_completed".equals(e.getEventType()) ||
                        "assessment_started".equals(e.getEventType()))
                .map(LearningEvent::getEntityId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new UserStatsResponse(
                totalHours,
                streak,
                lessonsCompleted,
                assessmentsTaken,
                events.size());
    }

    public List<DailyActivityResponse> getActivity(UUID userId, LocalDate from, LocalDate to) {
        OffsetDateTime fromDateTime = from.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime toDateTime = to.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

        var events = eventRepository.findEvents(
                userId, null, null, null, fromDateTime, toDateTime,
                PageRequest.of(0, 10000)).getContent();

        // Group by date
        Map<LocalDate, List<LearningEvent>> byDay = events.stream()
                .collect(Collectors.groupingBy(e -> e.getOccurredAt().toLocalDate()));

        // Convert to response
        return byDay.entrySet().stream()
                .map(entry -> new DailyActivityResponse(
                        entry.getKey(),
                        entry.getValue().size(),
                        calculateDailyHours(entry.getValue())))
                .sorted(Comparator.comparing(DailyActivityResponse::date))
                .toList();
    }

    private double extractDurationSeconds(LearningEvent event) {
        if (event.getPayload() == null)
            return 0.0;

        // Try to extract duration from payload - handle both integer and string formats
        String payload = event.getPayload();
        try {
            // Expected format: {"durationSeconds": 120} or simple JSON string
            if (payload.contains("durationSeconds")) {
                String value = payload.split("durationSeconds")[1]
                        .replaceAll("[^0-9]", "");
                if (!value.isEmpty()) {
                    return Double.parseDouble(value);
                }
            }
        } catch (Exception e) {
            // Payload might not have duration - return 0
        }
        return 0.0;
    }

    private int calculateStreak(List<LearningEvent> events) {
        if (events.isEmpty())
            return 0;

        // Get unique days with activity
        Set<LocalDate> uniqueDays = events.stream()
                .map(e -> e.getOccurredAt().toLocalDate())
                .collect(Collectors.toSet());

        // Count consecutive days back from today
        LocalDate today = LocalDate.now();
        int streak = 0;

        // Check if there's activity today or yesterday
        if (!uniqueDays.contains(today) && !uniqueDays.contains(today.minusDays(1))) {
            return 0;
        }

        // Start from yesterday if no activity today
        LocalDate current = uniqueDays.contains(today) ? today : today.minusDays(1);

        while (uniqueDays.contains(current)) {
            streak++;
            current = current.minusDays(1);
        }

        return streak;
    }

    private double calculateDailyHours(List<LearningEvent> events) {
        return events.stream()
                .mapToDouble(this::extractDurationSeconds)
                .sum() / 3600.0;
    }
}
