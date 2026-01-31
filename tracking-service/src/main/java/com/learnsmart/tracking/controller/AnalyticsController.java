package com.learnsmart.tracking.controller;

import com.learnsmart.tracking.dto.DailyActivityResponse;
import com.learnsmart.tracking.dto.UserStatsResponse;
import com.learnsmart.tracking.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable UUID userId) {
        return ResponseEntity.ok(analyticsService.calculateStats(userId));
    }

    @GetMapping("/users/{userId}/activity")
    public ResponseEntity<List<DailyActivityResponse>> getUserActivity(
            @PathVariable UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null)
            from = LocalDate.now().minusMonths(1);
        if (to == null)
            to = LocalDate.now();

        return ResponseEntity.ok(analyticsService.getActivity(userId, from, to));
    }
}
