package com.james.taskflow.task;

import com.james.taskflow.task.dto.TaskSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class TaskSpecifications {
    private TaskSpecifications() {}

    public static Specification<Task> from(TaskSearchCriteria c) {
        Specification<Task> spec = (root, query, cb) -> cb.conjunction();
        if (c.status() != null) {
            spec = spec.and(hasStatus(c.status()));
        }
        if (c.q() != null && !c.q().isBlank()) {
            spec = spec.and(titleContains(c.q().trim()));
        }
        if (c.dueBefore() != null) {
            spec = spec.and(dueBefore(c.dueBefore()));
        }
        return spec;
    }

    private static Specification<Task> hasStatus(TaskStatus status) {
            return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Task> titleContains(String text) {
            String escaped = text.toLowerCase()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            return (root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), "%" + escaped + "%", '\\');
    }

    private static Specification<Task> dueBefore(LocalDate date) {
            return (root, query, cb) -> cb.lessThan(root.get("dueDate"), date);
    }

}
