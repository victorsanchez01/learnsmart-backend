package com.learnsmart.content.controller;

import com.learnsmart.content.model.ContentItem;
import com.learnsmart.content.model.Domain;
import com.learnsmart.content.dto.ContentDtos;
import com.learnsmart.content.service.ContentItemService;
import com.learnsmart.content.service.DomainService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/content-items")
@RequiredArgsConstructor
public class ContentItemController {

    private final ContentItemService contentService;
    private final DomainService domainService;

    @GetMapping
    public List<ContentDtos.ContentItemResponse> getContentItems(@RequestParam(required = false) UUID domainId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return contentService.findAll(domainId, null, type, true, page, size)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ContentItem> createContentItem(@RequestBody ContentDtos.ContentItemInput input) {
        Domain domain = domainService.findById(input.getDomainId())
                .orElseThrow(() -> new RuntimeException("Domain not found"));

        ContentItem c = new ContentItem();
        c.setDomain(domain);
        c.setType(input.getType());
        c.setTitle(input.getTitle());
        c.setDescription(input.getDescription());
        c.setEstimatedMinutes(input.getEstimatedMinutes());
        c.setDifficulty(input.getDifficulty());
        c.setMetadata(input.getMetadata());
        c.setActive(input.isActive());

        return new ResponseEntity<>(contentService.create(c), HttpStatus.CREATED);
    }

    @PostMapping("/generate")
    public ResponseEntity<List<ContentItem>> generateContent(@RequestBody ContentDtos.GenerateContentInput input) {
        return new ResponseEntity<>(
                contentService.generateAndSave(input.getDomainId(), input.getNLessons(), input.getTopic()),
                HttpStatus.CREATED);
    }

    private ContentDtos.ContentItemResponse toDto(ContentItem item) {
        ContentDtos.ContentItemResponse dto = new ContentDtos.ContentItemResponse();
        dto.setId(item.getId());
        dto.setType(item.getType());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setEstimatedMinutes(item.getEstimatedMinutes());
        dto.setDifficulty(item.getDifficulty());
        dto.setMetadata(item.getMetadata());
        dto.setActive(item.isActive());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());

        if (item.getDomain() != null) {
            ContentDtos.DomainInput d = new ContentDtos.DomainInput();
            d.setCode(item.getDomain().getCode());
            d.setName(item.getDomain().getName());
            d.setDescription(item.getDomain().getDescription());
            dto.setDomain(d);
        }
        return dto;
    }
}
