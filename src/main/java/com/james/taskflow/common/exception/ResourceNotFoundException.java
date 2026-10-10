package com.james.taskflow.common.exception;

import jakarta.validation.constraints.NotBlank;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id " + id);
    }
}
