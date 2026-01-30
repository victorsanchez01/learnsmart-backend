package com.learnsmart.content.controller;

import com.learnsmart.content.model.Domain;
import com.learnsmart.content.dto.ContentDtos;
import com.learnsmart.content.service.DomainService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/domains")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService domainService;

    @GetMapping
    public List<Domain> getDomains(@RequestParam(required = false) String code,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return domainService.findAll(code, page, size);
    }

    @PostMapping
    public ResponseEntity<Domain> createDomain(@RequestBody ContentDtos.DomainInput input) {
        Domain d = new Domain();
        d.setCode(input.getCode());
        d.setName(input.getName());
        d.setDescription(input.getDescription());
        return new ResponseEntity<>(domainService.create(d), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Domain> getDomain(@PathVariable UUID id) {
        return domainService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
