package com.james.taskflow.task;

import com.james.taskflow.common.PageResponse;
import com.james.taskflow.common.exception.BadRequestException;
import com.james.taskflow.common.exception.ResourceNotFoundException;
import com.james.taskflow.task.dto.CreateTaskRequest;
import com.james.taskflow.task.dto.TaskResponse;
import com.james.taskflow.task.dto.TaskSearchCriteria;
import com.james.taskflow.task.dto.UpdateTaskRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "updatedAt", "dueDate", "title", "status");
    private final TaskRepository taskRepository;
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest req) {
        Task task = new Task(req.title(), req.description(), req.dueDate());
        return TaskResponse.from(taskRepository.save(task));
    }

   @Transactional(readOnly = true)
   public PageResponse<TaskResponse> search(TaskSearchCriteria criteria, Pageable pageable) {
        validateSort(pageable.getSort());

        Pageable stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(Sort.by("id")));
        Page<Task> page = taskRepository.findAll(TaskSpecifications.from(criteria), stable);
        return PageResponse.from(page.map(TaskResponse::from));
   }

   private void validateSort(Sort sort) {
        sort.forEach(order -> {
            if(!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE_FIELDS);
            }
        });
   }

    @Transactional(readOnly = true)
    public TaskResponse findById(UUID id){
        return TaskResponse.from(getOrThrow(id));
    }

    @Transactional
    public TaskResponse update(UUID id, UpdateTaskRequest req) {
        Task task = getOrThrow(id);
        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setStatus(req.status());
        task.setDueDate(req.dueDate());
        return TaskResponse.from(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public void delete(UUID id){
        taskRepository.delete(getOrThrow(id));
    }

    private Task getOrThrow(UUID id){
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

}
