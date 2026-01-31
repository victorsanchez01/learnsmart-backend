package com.learnsmart.tracking.dto;

public record UserStatsResponse(
        double totalHours,
        int currentStreak,
        long lessonsCompleted,
        long assessmentsTaken,
        long totalEvents) {
}
