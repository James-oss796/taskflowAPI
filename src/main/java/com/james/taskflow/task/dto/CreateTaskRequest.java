package com.james.taskflow.task.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @Size(max = 5000, message = "description must be at most 5000 characters")
        String description,

        @FutureOrPresent(message = "dueDate cannot be in the past")
        LocalDate dueDate
) {}
