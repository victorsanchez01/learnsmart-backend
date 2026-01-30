package com.learnsmart.content.controller;

import com.learnsmart.content.model.Skill;
import com.learnsmart.content.model.Domain;
import com.learnsmart.content.dto.ContentDtos;
import com.learnsmart.content.service.SkillService;
import com.learnsmart.content.service.DomainService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;
    private final DomainService domainService;

    @GetMapping
    public List<Skill> getSkills(@RequestParam(required = false) UUID domainId,
            @RequestParam(required = false) String code,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return skillService.findAll(domainId, code, null, page, size);
    }

    @PostMapping
    public ResponseEntity<Skill> createSkill(@RequestBody ContentDtos.SkillInput input) {
        Domain domain = domainService.findById(input.getDomainId())
                .orElseThrow(() -> new RuntimeException("Domain not found"));

        Skill s = new Skill();
        s.setDomain(domain);
        s.setCode(input.getCode());
        s.setName(input.getName());
        s.setDescription(input.getDescription());
        s.setLevel(input.getLevel());
        s.setTags(input.getTags());

        return new ResponseEntity<>(skillService.create(s), HttpStatus.CREATED);
    }
}
