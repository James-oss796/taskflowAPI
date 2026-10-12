package com.james.taskflow.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(UUID id, boolean admin) {
    public static AuthenticatedUser from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new AuthenticatedUser(UUID.fromString(jwt.getSubject()),
                roles != null && roles.contains("ADMIN"));
    }
}