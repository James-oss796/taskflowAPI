package com.james.taskflow.task;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc   // see the import note below
@Testcontainers
@ActiveProfiles("test")
@WithMockUser
class TaskApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired TaskRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createTask_returns201_withLocationAndGeneratedFields() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Write tests","dueDate":"2099-01-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createTask_withBlankTitle_returns400_withFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void getTask_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_paginates() throws Exception {
        for (int i = 0; i < 25; i++) {
            repository.save(new Task("Task " + i, null, null));
        }

        mockMvc.perform(get("/api/v1/tasks").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void search_filtersByStatus() throws Exception {
        Task done = new Task("Finished", null, null);
        done.setStatus(TaskStatus.DONE);
        repository.save(done);
        repository.save(new Task("Pending", null, null));

        mockMvc.perform(get("/api/v1/tasks").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Finished"));
    }

    @Test
    void everyResponse_carriesRequestId_andReusesValidIncomingOne() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(header().exists("X-Request-Id"));

        mockMvc.perform(get("/api/v1/tasks").header("X-Request-Id", "abc-12345678"))
                .andExpect(header().string("X-Request-Id", "abc-12345678"));

        mockMvc.perform(get("/api/v1/tasks").header("X-Request-Id", "bad id\nforged log line"))
                .andExpect(header().string("X-Request-Id",
                        org.hamcrest.Matchers.not("bad id\nforged log line")));
    }

    @Test
    void search_treatsPercentSignAsLiteralText() throws Exception {
        repository.save(new Task("100% done", null, null));
        repository.save(new Task("1000 tasks", null, null));

        mockMvc.perform(get("/api/v1/tasks").param("q", "100%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("100% done"));
    }

    @Test
    void search_withInvalidInputs_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").param("page", "dsffafd"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/tasks").param("size", "5000"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/tasks").param("sortBy", "password"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/tasks").param("direction", "sideways"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagination_withIdenticalSortValues_neverRepeatsOrSkipsItems() throws Exception {
        LocalDate sameDate = LocalDate.of(2099, 1, 1);
        for (int i = 0; i < 30; i++) {
            repository.save(new Task("Task " + i, null, sameDate));
        }

        Set<String> seenIds = new HashSet<>();
        for (int page = 0; page < 3; page++) {
            String body = mockMvc.perform(get("/api/v1/tasks")
                            .param("page", String.valueOf(page))
                            .param("size", "10")
                            .param("sortBy", "dueDate"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            List<String> ids = JsonPath.read(body, "$.content[*].id");
            seenIds.addAll(ids);
        }

        assertThat(seenIds).hasSize(30);   // the tie-breaker test
    }
}
