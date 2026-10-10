package com.james.taskflow.task.dto;

import com.james.taskflow.task.TaskStatus;

import java.time.LocalDate;

public record TaskSearchCriteria(
        TaskStatus status,
        String q,
        LocalDate dueBefore
) {
}
