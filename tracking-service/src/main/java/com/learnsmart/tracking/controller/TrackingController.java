package com.learnsmart.tracking.controller;

import com.learnsmart.tracking.model.LearningEvent;
import com.learnsmart.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService service;

    @PostMapping
    public ResponseEntity<LearningEvent> createEvent(@RequestBody LearningEvent event) {
        LearningEvent created = service.createEvent(event);
        return ResponseEntity.created(URI.create("/events/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<LearningEvent>> listEvents(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            Pageable pageable) {
        return ResponseEntity.ok(service.listEvents(userId, eventType, entityType, entityId, from, to, pageable));
    }
}
