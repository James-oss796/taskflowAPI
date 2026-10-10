package com.james.taskflow.task;

import com.james.taskflow.common.exception.BadRequestException;
import com.james.taskflow.common.exception.ResourceNotFoundException;
import com.james.taskflow.task.dto.CreateTaskRequest;
import com.james.taskflow.task.dto.TaskResponse;
import com.james.taskflow.task.dto.TaskSearchCriteria;
import com.james.taskflow.task.dto.UpdateTaskRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;

    @InjectMocks
    TaskService taskService;

    @Test
    void create_savesTaskAndReturnsResponse(){
        var request = new CreateTaskRequest("Learn testing", "desc", null);
        when(taskRepository.saveAndFlush(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse taskResponse = taskService.create(request);

        assertThat(taskResponse.title()).isEqualTo("Learn testing");
        assertThat(taskResponse.status()).isEqualTo(TaskStatus.TODO);
        verify(taskRepository).saveAndFlush(any(Task.class));
    }

    @Test
    void findById_whenMissing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void update_changesAllEditableFields() {
        UUID id = UUID.randomUUID();
        Task existing = new Task("Old", "old desc", null);
        when(taskRepository.findById(id)).thenReturn(Optional.of(existing));
        when(taskRepository.saveAndFlush(existing)).thenReturn(existing);

        var request = new UpdateTaskRequest("New", "new desc", TaskStatus.DONE, null);
        TaskResponse response = taskService.update(id, request);

        assertThat(response.title()).isEqualTo("New");
        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void search_withNonWhitelistedSort_isRejectedBeforeTouchingDatabase() {
        var pageable = PageRequest.of(0, 10, Sort.by("password"));
        var criteria = new TaskSearchCriteria(null, null, null);

        assertThatThrownBy(() -> taskService.search(criteria, pageable))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(taskRepository);
    }
}
