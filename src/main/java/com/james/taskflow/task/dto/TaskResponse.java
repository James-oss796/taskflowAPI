package com.james.taskflow.task.dto;

import com.james.taskflow.task.Task;
import com.james.taskflow.task.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getDueDate(),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }
}
