package com.james.taskflow.task;

import com.james.taskflow.common.PageResponse;
import com.james.taskflow.common.exception.BadRequestException;
import com.james.taskflow.task.dto.CreateTaskRequest;
import com.james.taskflow.task.dto.TaskResponse;
import com.james.taskflow.task.dto.TaskSearchCriteria;
import com.james.taskflow.task.dto.UpdateTaskRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@Tag(name= "Tasks", description = "Create, read, update, delete and search tasks")
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Create a task")
    @ApiResponse(responseCode = "201", description = "Task Created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest req) {
        TaskResponse created = taskService.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Search tasks with filters, pagination and sorting")
    @GetMapping
    public PageResponse<TaskResponse> search(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new BadRequestException("direction must be 'asc' or 'desc'"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        return taskService.search(new TaskSearchCriteria(status, q, dueBefore), pageable);
    }

    @Operation(summary = "Find a task by Id")
    @GetMapping("/{id}")
    public TaskResponse findById(@PathVariable UUID id) {
        return taskService.findById(id);
    }

    @Operation(summary = "Update a task")
    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest req) {
        return taskService.update(id, req);
    }

    @Operation(summary = "Delete a task")
    @ApiResponse(responseCode = "200", description = "Task deleted")
    @ApiResponse(responseCode = "400", description = "Deletion failed")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        taskService.delete(id);
    }
}
