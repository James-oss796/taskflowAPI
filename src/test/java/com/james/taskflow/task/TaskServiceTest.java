package com.james.taskflow.task;

import com.james.taskflow.common.exception.BadRequestException;
import com.james.taskflow.common.exception.ResourceNotFoundException;
import com.james.taskflow.security.AuthenticatedUser;
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
class TaskServiceTest {

    @Mock TaskRepository repository;
    @InjectMocks TaskService service;

    static final UUID OWNER = UUID.randomUUID();
    final AuthenticatedUser owner = new AuthenticatedUser(OWNER, false);
    final AuthenticatedUser stranger = new AuthenticatedUser(UUID.randomUUID(), false);
    final AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), true);

    @Test
    void create_assignsTheCurrentUserAsOwner() {
        when(repository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));
        TaskResponse response = service.create(new CreateTaskRequest("Learn", "desc", null), owner);
        assertThat(response.ownerId()).isEqualTo(OWNER);
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void findById_ofAnotherUsersTask_looksLikeNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new Task("secret", null, null, OWNER)));
        assertThatThrownBy(() -> service.findById(id, stranger)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_asAdmin_canReadAnyTask() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new Task("secret", null, null, OWNER)));
        assertThat(service.findById(id, admin).title()).isEqualTo("secret");
    }

    @Test
    void update_byStranger_isNotFound_andNothingIsSaved() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new Task("t", null, null, OWNER)));
        var req = new UpdateTaskRequest("hacked", null, TaskStatus.DONE, null);
        assertThatThrownBy(() -> service.update(id, req, stranger)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void search_withNonWhitelistedSort_isRejected_beforeTouchingTheDatabase() {
        var pageable = PageRequest.of(0, 10, Sort.by("password"));
        assertThatThrownBy(() -> service.search(new TaskSearchCriteria(null, null, null), pageable, owner))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository);
    }
}