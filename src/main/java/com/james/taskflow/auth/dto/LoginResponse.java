package com.james.taskflow.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {
    @Override
    public String toString() {
        return "LoginResponse[accessToken=***, refreshToken=***, tokenType=" + tokenType
                + ", expiresInSeconds=" + expiresInSeconds + "]";
    }
}