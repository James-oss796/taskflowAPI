package com.james.taskflow.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.login-protection")
public record LoginProtectionProperties(
        @DefaultValue("5") int maxFailedAttempts,
        @DefaultValue("15m") Duration lockDuration) {}