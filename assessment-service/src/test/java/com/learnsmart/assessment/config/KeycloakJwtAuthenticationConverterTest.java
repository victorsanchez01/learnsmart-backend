package com.learnsmart.assessment.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(claims))
                .build();
    }

    // -------------------------------------------------------------------------
    // convert — realm_access null → no realm roles, only default authorities
    // -------------------------------------------------------------------------

    @Test
    void testConvert_NoRealmAccess_ReturnsDefaultAuthoritiesOnly() {
        // No realm_access claim present
        Jwt jwt = buildJwt(Map.of("sub", "user-123"));

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertNotNull(token);
        // Default JwtGrantedAuthoritiesConverter emits no authorities when no scope
        // claim
        // is present either, so total authorities may be empty or just default → no
        // ROLE_* entries
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) token.getAuthorities();
        boolean hasRoleAuthority = authorities.stream()
                .anyMatch(a -> a.getAuthority().startsWith("ROLE_"));
        assertFalse(hasRoleAuthority, "No ROLE_* authorities expected when realm_access is absent");
    }

    // -------------------------------------------------------------------------
    // convert — realm_access present with roles → ROLE_ authorities added
    // -------------------------------------------------------------------------

    @Test
    void testConvert_WithRealmRoles_MapsToRoleAuthorities() {
        Map<String, Object> realmAccess = Map.of("roles", List.of("admin", "user"));
        Jwt jwt = buildJwt(Map.of(
                "sub", "user-456",
                "realm_access", realmAccess));

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertNotNull(token);
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) token.getAuthorities();
        List<String> authorityNames = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertTrue(authorityNames.contains("ROLE_ADMIN"),
                "Expected ROLE_ADMIN from realm role 'admin'");
        assertTrue(authorityNames.contains("ROLE_USER"),
                "Expected ROLE_USER from realm role 'user'");
    }

    // -------------------------------------------------------------------------
    // convert — realm_access present but empty roles list → no ROLE_ from realm
    // -------------------------------------------------------------------------

    @Test
    void testConvert_EmptyRealmRoles_NoRoleAuthorities() {
        Map<String, Object> realmAccess = Map.of("roles", List.of());
        Jwt jwt = buildJwt(Map.of(
                "sub", "user-789",
                "realm_access", realmAccess));

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertNotNull(token);
        // No realm roles → no ROLE_* authorities from realm
        boolean hasRealmRole = ((Collection<GrantedAuthority>) token.getAuthorities()).stream()
                .anyMatch(a -> a.getAuthority().startsWith("ROLE_"));
        assertFalse(hasRealmRole);
    }
}
