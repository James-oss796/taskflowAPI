package com.james.taskflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// auth/dto/RefreshRequest.java
public record RefreshRequest(
        @NotBlank(message = "refreshToken is required")
        @Size(max = 200, message = "refreshToken is too long")
        String refreshToken
) {
    @Override
    public String toString() {
        return "RefreshRequest[refreshToken=***]";
    }
}