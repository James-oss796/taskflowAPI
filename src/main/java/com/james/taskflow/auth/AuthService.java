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
import com.james.taskflow.user.User;
import com.james.taskflow.user.UserRepository;
import com.james.taskflow.user.dto.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash;
    private final RefreshTokenService refreshTokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,  JwtService jwtService, RefreshTokenService refreshTokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = users.findByEmail(normalize(request.email())).orElse(null);

        String hashToCheck = (user != null) ? user.getPasswordHash() : dummyHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null || !passwordMatches || !user.isEnabled()) {
            throw new InvalidCredentialsException();
        }


        JwtService.IssuedToken access = jwtService.issueAccessToken(user);
        String refresh = refreshTokens.issueNewFamily(user.getId());
        return new LoginResponse(access.value(), refresh, "Bearer", access.expiresInSeconds());
    }


    // Deliberately NOT @Transactional: rotation commits on its own before anything else can fail,
// and a thrown exception here can never roll back a family revocation.
    public LoginResponse refresh(RefreshRequest request) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(request.refreshToken());

        User user = users.findById(rotation.userId()).filter(User::isEnabled).orElse(null);
        if (user == null) {                                  // account deleted or disabled meanwhile
            refreshTokens.revokeFamily(rotation.familyId());
            throw new InvalidRefreshTokenException();
        }

        JwtService.IssuedToken access = jwtService.issueAccessToken(user);
        return new LoginResponse(access.value(), rotation.newRawToken(), "Bearer", access.expiresInSeconds());
    }

    public void logout(RefreshRequest request) {
        refreshTokens.revoke(request.refreshToken());
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        String email = normalize(req.email());

        if(users.existsByEmail(email)) {
            throw new ConflictException("Email is already registered!");
        }

        User user = new User(email, passwordEncoder.encode(req.password()));

        try{
            return UserResponse.from(users.saveAndFlush(user));
        }catch(DataIntegrityViolationException ex){
            throw new ConflictException("Email is already registered!");
        }
    }

    private static String normalize(String email){
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
