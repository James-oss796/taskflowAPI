package com.james.taskflow.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
@Validated
public record JwtProperties (
        @NotBlank String secret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl
) {
    @Override
    public String toString() {
        return "JwtProperties[secret=***, issuer=" + issuer + ", accessTokenTtl=" + accessTokenTtl + "]";
    }
}
