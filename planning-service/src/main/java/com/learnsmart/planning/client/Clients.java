package com.learnsmart.planning.client;

import com.learnsmart.planning.dto.ExternalDtos;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import java.util.UUID;

public interface Clients {

    @FeignClient(name = "profile-service")
    public interface ProfileClient {
        @GetMapping("/profiles/{id}")
        ExternalDtos.UserProfile getProfile(@PathVariable("id") String id);

        // Sometimes valid to search by userId String
        @GetMapping("/profiles/uid/{userId}")
        ExternalDtos.UserProfile getProfileByUserId(@PathVariable("userId") String userId);
    }

    @FeignClient(name = "content-service")
    public interface ContentClient {
        @GetMapping("/content-items")
        List<ExternalDtos.ContentItemDto> getContentItems(@RequestParam(value = "size", defaultValue = "100") int size);
    }

    @FeignClient(name = "ai-service", url = "${AI_SERVICE_URL:http://ai-service:8000}", path = "/v1")
    public interface AiClient {
        @PostMapping("/plans/generate")
        ExternalDtos.GeneratePlanResponse generatePlan(@RequestBody ExternalDtos.GeneratePlanRequest request);
    }
}
