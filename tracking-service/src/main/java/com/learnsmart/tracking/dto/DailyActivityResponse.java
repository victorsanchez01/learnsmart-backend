package com.learnsmart.tracking.dto;

import java.time.LocalDate;

public record DailyActivityResponse(
        LocalDate date,
        int eventCount,
        double hoursStudied) {
}
