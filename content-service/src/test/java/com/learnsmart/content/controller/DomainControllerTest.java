package com.learnsmart.content.controller;

import com.learnsmart.content.dto.ContentDtos;
import com.learnsmart.content.model.Domain;
import com.learnsmart.content.service.DomainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomainControllerTest {

    @Mock
    private DomainService domainService;

    @InjectMocks
    private DomainController controller;

    @Test
    void testGetDomains() {
        String code = "MATH";
        when(domainService.findAll(eq(code), any(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

        List<Domain> result = controller.getDomains(code, "published", 0, 10);
        assertTrue(result.isEmpty());
        verify(domainService).findAll(eq(code), eq("published"), eq(0), eq(10));
    }

    // -------------------------------------------------------------------------
    // getDomains — status = "all" → passes null to service (branch coverage)
    // -------------------------------------------------------------------------

    @Test
    void testGetDomains_StatusAll_PassesNullFilter() {
        when(domainService.findAll(isNull(), isNull(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

        List<Domain> result = controller.getDomains(null, "all", 0, 10);
        assertTrue(result.isEmpty());
        verify(domainService).findAll(null, null, 0, 10);
    }

    @Test
    void testCreateDomain() {
        ContentDtos.DomainInput input = new ContentDtos.DomainInput();
        input.setCode("MATH");
        input.setName("Math");

        when(domainService.create(any(Domain.class))).thenAnswer(i -> {
            Domain d = i.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        ResponseEntity<Domain> response = controller.createDomain(input);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("MATH", response.getBody().getCode());
    }

    @Test
    void testGetDomain_Found() {
        UUID id = UUID.randomUUID();
        Domain domain = new Domain();
        domain.setId(id);

        when(domainService.findById(id)).thenReturn(Optional.of(domain));

        ResponseEntity<Domain> response = controller.getDomain(id);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void testGetDomain_NotFound() {
        UUID id = UUID.randomUUID();
        when(domainService.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<Domain> response = controller.getDomain(id);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
