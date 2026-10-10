package com.james.taskflow.task.dto;

import com.james.taskflow.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
public record UpdateTaskRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @Size(max = 5000, message = "description must be at most 5000 characters")
        String description,

        @NotNull(message = "status is required")
        TaskStatus status,

        LocalDate dueDate
) {
}
