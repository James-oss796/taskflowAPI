package com.james.taskflow.user.dto;

import com.james.taskflow.user.Role;
import com.james.taskflow.user.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse (
        UUID id, String email, Role role, Instant createdAt
){
    public static UserResponse from(User u){
        return new UserResponse(u.getId(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }
}
