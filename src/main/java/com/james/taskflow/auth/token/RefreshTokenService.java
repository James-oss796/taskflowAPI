// auth/token/RefreshTokenService.java
package com.james.taskflow.auth.token;

import com.james.taskflow.common.exception.InvalidRefreshTokenException;
import com.james.taskflow.security.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties props;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties props) {
        this.repository = repository;
        this.props = props;
    }

    /** The result of a successful rotation. */
    public record Rotation(UUID userId, UUID familyId, String newRawToken) {}

    /** Start a new family (called at login). Returns the raw token to give to the client. */
    @Transactional
    public String issueNewFamily(UUID userId) {
        return create(userId, UUID.randomUUID());
    }

    /**
     * Consume a refresh token and issue its successor in the same family.
     * noRollbackFor is essential: when we detect reuse we revoke the family and THEN throw.
     * Without it, the exception would roll back the revocation we just made.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String rawToken) {
        Instant now = Instant.now();

        RefreshToken current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException();            // already dead
        }
        if (current.getUsedAt() != null) {
            // A spent token came back: it was copied. Kill the whole family.
            repository.revokeFamily(current.getFamilyId(), now);
            log.warn("Refresh token reuse detected; family {} revoked", current.getFamilyId());
            throw new InvalidRefreshTokenException();
        }
        if (current.getExpiresAt().isBefore(now)) {
            throw new InvalidRefreshTokenException();
        }

        // Atomic single-use: only one concurrent request can flip used_at.
        if (repository.markUsed(current.getId(), now) == 0) {
            repository.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }

        String next = create(current.getUserId(), current.getFamilyId());
        return new Rotation(current.getUserId(), current.getFamilyId(), next);
    }

    /** Logout: revoke the whole family. Silent if the token is unknown (no information leak). */
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), Instant.now()));
    }

    @Transactional
    public void revokeFamily(UUID familyId) {
        repository.revokeFamily(familyId, Instant.now());
    }

    private String create(UUID userId, UUID familyId) {
        String raw = generate();
        repository.save(new RefreshToken(userId, familyId, hash(raw),
                Instant.now().plus(props.refreshTokenTtl())));
        return raw;
    }

    private static String generate() {
        byte[] bytes = new byte[32];                       // 256 bits
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String raw) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}