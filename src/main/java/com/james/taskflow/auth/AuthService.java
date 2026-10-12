package com.james.taskflow.auth;

import com.james.taskflow.auth.dto.LoginRequest;
import com.james.taskflow.auth.dto.LoginResponse;
import com.james.taskflow.auth.dto.RefreshRequest;
import com.james.taskflow.auth.dto.RegisterRequest;
import com.james.taskflow.auth.token.RefreshTokenService;
import com.james.taskflow.common.exception.ConflictException;
import com.james.taskflow.common.exception.InvalidCredentialsException;
import com.james.taskflow.common.exception.InvalidRefreshTokenException;
import com.james.taskflow.security.JwtService;
import com.james.taskflow.security.LoginProtectionProperties;
import com.james.taskflow.user.Role;
import com.james.taskflow.user.User;
import com.james.taskflow.user.UserRepository;
import com.james.taskflow.user.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final LoginProtectionProperties protection;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       RefreshTokenService refreshTokens, LoginProtectionProperties protection) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.protection = protection;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = new User(email, passwordEncoder.encode(request.password()), Role.USER);
        try {
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email is already registered");
        }
    }

    // noRollbackFor: we WRITE the failure counter and then THROW. A rollback would erase the evidence.
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResponse login(LoginRequest request) {
        Instant now = Instant.now();
        User user = users.findByEmail(normalize(request.email())).orElse(null);

        // always one BCrypt comparison, so timing never reveals whether the account exists
        String hashToCheck = (user != null) ? user.getPasswordHash() : dummyHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null) {
            log.warn("Login failed: unknown email");
            throw new InvalidCredentialsException();
        }
        if (user.isLockedAt(now)) {
            log.warn("Login rejected: account {} is locked", user.getId());
            throw new InvalidCredentialsException();          // even the correct password is refused
        }
        if (!passwordMatches) {
            recordFailure(user, now);
            throw new InvalidCredentialsException();
        }
        if (!user.isEnabled()) {
            log.warn("Login rejected: account {} is disabled", user.getId());
            throw new InvalidCredentialsException();
        }

        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            users.clearFailures(user.getId());
        }
        JwtService.IssuedToken access = jwtService.issueAccessToken(user);
        String refresh = refreshTokens.issueNewFamily(user.getId());
        log.info("Login succeeded for user {}", user.getId());
        return new LoginResponse(access.value(), refresh, "Bearer", access.expiresInSeconds());
    }

    private void recordFailure(User user, Instant now) {
        users.incrementFailedAttempts(user.getId());
        int attempts = users.findFailedAttempts(user.getId());   // re-read: the row is locked by our transaction
        if (attempts >= protection.maxFailedAttempts()) {
            users.lock(user.getId(), now.plus(protection.lockDuration()));
            log.warn("Account {} locked after {} failed logins", user.getId(), attempts);
        } else {
            log.warn("Login failed for user {} (attempt {})", user.getId(), attempts);
        }
    }

    // Deliberately not @Transactional: rotation commits on its own.
    public LoginResponse refresh(RefreshRequest request) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(request.refreshToken());
        User user = users.findById(rotation.userId()).filter(User::isEnabled).orElse(null);
        if (user == null) {
            refreshTokens.revokeFamily(rotation.familyId());
            throw new InvalidRefreshTokenException();
        }
        JwtService.IssuedToken access = jwtService.issueAccessToken(user);
        return new LoginResponse(access.value(), rotation.newRawToken(), "Bearer", access.expiresInSeconds());
    }

    public void logout(RefreshRequest request) {
        refreshTokens.revoke(request.refreshToken());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}