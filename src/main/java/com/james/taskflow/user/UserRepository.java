package com.james.taskflow.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.failedLoginAttempts = u.failedLoginAttempts + 1 where u.id = :id")
    void incrementFailedAttempts(@Param("id") UUID id);

    @Query("select u.failedLoginAttempts from User u where u.id = :id")
    int findFailedAttempts(@Param("id") UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.lockedUntil = :until, u.failedLoginAttempts = 0 where u.id = :id")
    void lock(@Param("id") UUID id, @Param("until") Instant until);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.failedLoginAttempts = 0, u.lockedUntil = null where u.id = :id")
    void clearFailures(@Param("id") UUID id);
}
