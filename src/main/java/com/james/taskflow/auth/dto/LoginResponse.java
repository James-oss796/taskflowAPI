package com.james.taskflow.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
