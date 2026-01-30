package com.learnsmart.assessment.controller;

import com.learnsmart.assessment.model.*;
import com.learnsmart.assessment.service.AssessmentSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentSessionService sessionService;

    @PostMapping("/sessions")
    public ResponseEntity<AssessmentSession> createSession(@RequestBody AssessmentSession session) {
        return ResponseEntity.status(201).body(sessionService.createSession(session));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<AssessmentSession> getSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    @PutMapping("/sessions/{sessionId}/status")
    public ResponseEntity<AssessmentSession> updateStatus(@PathVariable UUID sessionId, @RequestParam String status) {
        return ResponseEntity.ok(sessionService.updateStatus(sessionId, status));
    }

    @GetMapping("/sessions/{sessionId}/next-item")
    public ResponseEntity<AssessmentItem> getNextItem(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.getNextItem(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/responses")
    public ResponseEntity<UserItemResponseWithFeedback> submitResponse(
            @PathVariable UUID sessionId,
            @RequestBody SubmitResponseRequest request) {
        return ResponseEntity.ok(sessionService.submitResponse(sessionId, request));
    }

    @GetMapping("/sessions/{sessionId}/responses")
    public ResponseEntity<List<UserItemResponse>> getSessionResponses(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.getSessionResponses(sessionId));
    }
}
