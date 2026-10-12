package com.james.taskflow.auth.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
public class RefreshTokenCleanup {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanup.class);
    private final RefreshTokenRepository repository;

    public RefreshTokenCleanup(RefreshTokenRepository repository) { this.repository = repository; }

    @Scheduled(cron = "0 30 3 * * *")   // every night at 03:30
    @Transactional
    public void purge() {
        int removed = repository.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(1)));
        log.info("Purged {} expired refresh tokens", removed);
    }
}