package com.learnsmart.assessment.client;

import com.learnsmart.assessment.dto.MasteryDtos;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@FeignClient(name = "content-service")
public interface ContentClient {

    @GetMapping("/skills/{id}")
    MasteryDtos.SkillInfo getSkill(@PathVariable("id") UUID id);
}
