package com.james.taskflow.auth;

import com.james.taskflow.auth.dto.LoginRequest;
import com.james.taskflow.auth.dto.LoginResponse;
import com.james.taskflow.auth.dto.RegisterRequest;
import com.james.taskflow.common.exception.ConflictException;
import com.james.taskflow.common.exception.InvalidCredentialsException;
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

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,  JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = users.findByEmail(normalize(request.email())).orElse(null);

        String hashToCheck = (user != null) ? user.getPasswordHash() : dummyHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null || !passwordMatches || !user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        JwtService.IssuedToken token = jwtService.issueAccessToken(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds());
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
