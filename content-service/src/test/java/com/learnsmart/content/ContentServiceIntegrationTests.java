package com.learnsmart.content;

import com.learnsmart.content.dto.ContentDtos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentServiceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullContentFlow() throws Exception {
        // 1. Create Domain
        ContentDtos.DomainInput domainInput = new ContentDtos.DomainInput();
        domainInput.setCode("math");
        domainInput.setName("Mathematics");
        domainInput.setDescription("Math Domain");

        MvcResult domainResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(domainInput)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String domainIdStr = objectMapper.readTree(domainResult.getResponse().getContentAsString()).get("id").asText();
        UUID domainId = UUID.fromString(domainIdStr);

        // 2. Create Skill
        ContentDtos.SkillInput skillInput = new ContentDtos.SkillInput();
        skillInput.setDomainId(domainId);
        skillInput.setCode("algebra-1");
        skillInput.setName("Algebra I");
        skillInput.setDescription("Basic Algebra");
        skillInput.setLevel("A1");
        skillInput.setTags(List.of("basic", "math"));

        mockMvc.perform(post("/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(skillInput)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").value("algebra-1"));

        // 3. Create ContentItem
        ContentDtos.ContentItemInput contentInput = new ContentDtos.ContentItemInput();
        contentInput.setDomainId(domainId);
        contentInput.setType("lesson");
        contentInput.setTitle("Intro to Algebra");
        contentInput.setDescription("Learning variables");
        contentInput.setEstimatedMinutes(15);
        contentInput.setDifficulty(new BigDecimal("0.50"));
        contentInput.setMetadata("{\"url\": \"http://video.com\"}");
        contentInput.setActive(true);

        mockMvc.perform(post("/content-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contentInput)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Intro to Algebra"));
    }
}
