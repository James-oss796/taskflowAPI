package com.james.taskflow.auth;

import com.james.taskflow.auth.dto.CurrentUserResponse;
import com.james.taskflow.auth.dto.LoginRequest;
import com.james.taskflow.auth.dto.LoginResponse;
import com.james.taskflow.auth.dto.RegisterRequest;
import com.james.taskflow.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name="Auth", description = "Registration and authentication")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Log in and receive an access token")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Who am I? Returns the identity carried by the token")
    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new CurrentUserResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsStringList("roles")
        );
    }
}
