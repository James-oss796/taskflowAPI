package com.james.taskflow.auth;

import com.james.taskflow.support.WithJwtUser;
import com.james.taskflow.task.Task;
import com.james.taskflow.task.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthorizationIntegrationTest {

    static final String ALICE = "00000000-0000-0000-0000-0000000000a1";
    static final String BOB   = "00000000-0000-0000-0000-0000000000b2";
    static final String ADMIN = "00000000-0000-0000-0000-0000000000c3";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TaskRepository tasks;
    @Autowired
    JdbcTemplate jdbc;

    UUID bobTaskId;

    private void insertUser(String id, String email, String role) {
        jdbc.update("insert into users (id, email, password_hash, role) values (?, ?, ?, ?)",
                UUID.fromString(id), email, "{noop}unused", role);
    }

    @BeforeEach
    void setUp() {
        jdbc.update("delete from users");
        insertUser(ALICE, "alice@example.com", "USER");
        insertUser(BOB, "bob@example.com", "USER");
        insertUser(ADMIN, "admin@example.com", "ADMIN");
        tasks.save(new Task("alice task", null, null, UUID.fromString(ALICE)));
        bobTaskId = tasks.save(new Task("bob task", null, null, UUID.fromString(BOB))).getId();
    }

    @Test
    @WithJwtUser(subject = ALICE)
    void userSeesOnlyOwnTasks() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("alice task"));
    }

    @Test
    @WithJwtUser(subject = ALICE)
    void getAnotherUsersTask_returns404_notForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/{id}", bobTaskId)).andExpect(status().isNotFound());
    }

    @Test
    @WithJwtUser(subject = ALICE)
    void updateAnotherUsersTask_returns404_andDataIsUnchanged() throws Exception {
        mockMvc.perform(put("/api/v1/tasks/{id}", bobTaskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"hacked\",\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());
        assertThat(tasks.findById(bobTaskId).orElseThrow().getTitle()).isEqualTo("bob task");
    }

    @Test
    @WithJwtUser(subject = ALICE)
    void deleteAnotherUsersTask_returns404_andTheTaskSurvives() throws Exception {
        mockMvc.perform(delete("/api/v1/tasks/{id}", bobTaskId)).andExpect(status().isNotFound());
        assertThat(tasks.findById(bobTaskId)).isPresent();
    }

    @Test
    @WithJwtUser(subject = ALICE)
    void createdTask_belongsToTheCaller() throws Exception {
        mockMvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"mine\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(ALICE));
    }

    @Test
    @WithJwtUser(subject = ADMIN, roles = {"ADMIN"})
    void adminSeesEveryonesTasks() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithJwtUser(subject = ALICE)
    void adminEndpoint_asUser_returns403_asProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));   // a 500 here means the trap is not fixed
    }

    @Test
    @WithJwtUser(subject = ADMIN, roles = {"ADMIN"})
    void adminEndpoint_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }
}