package com.james.taskflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank (message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        String password
) {
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", password=***]";
    }
}
