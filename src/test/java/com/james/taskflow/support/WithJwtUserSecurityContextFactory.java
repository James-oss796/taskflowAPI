package com.james.taskflow.support;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.List;

// WithJwtUserSecurityContextFactory.java
public class WithJwtUserSecurityContextFactory implements WithSecurityContextFactory<WithJwtUser> {
    @Override
    public SecurityContext createSecurityContext(WithJwtUser annotation) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(annotation.subject())
                .claim("roles", List.of(annotation.roles()))
                .build();
        List<GrantedAuthority> authorities = Arrays.stream(annotation.roles())
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(jwt, authorities));
        return context;
    }
}