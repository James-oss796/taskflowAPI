package com.james.taskflow.auth.dto;

import java.util.List;

public record CurrentUserResponse(String id, String email, List<String> roles) {
}
