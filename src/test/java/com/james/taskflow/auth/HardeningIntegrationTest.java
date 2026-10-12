package com.james.taskflow.auth;

import com.james.taskflow.TestContainersPostgres;
import com.james.taskflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class HardeningIntegrationTest extends TestContainersPostgres {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isCreated());
    }

    private int login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void accountLocksAfterFiveFailures_evenForTheCorrectPassword() throws Exception {
        register("lock@example.com", "correct-horse-battery");
        for (int i = 0; i < 5; i++) {
            assertThat(login("lock@example.com", "wrong-wrong-wrong")).isEqualTo(401);
        }
        assertThat(login("lock@example.com", "correct-horse-battery")).isEqualTo(401);   // locked
        // proves the lock was committed even though the request ended in an exception
        assertThat(userRepository.findByEmail("lock@example.com").orElseThrow().getLockedUntil()).isNotNull();
    }

    @Test
    void lockExpires_andSuccessResetsTheCounter() throws Exception {
        register("unlock@example.com", "correct-horse-battery");
        for (int i = 0; i < 5; i++) login("unlock@example.com", "wrong-wrong-wrong");

        jdbc.update("update users set locked_until = now() - interval '1 minute' where email = ?", "unlock@example.com");

        assertThat(login("unlock@example.com", "correct-horse-battery")).isEqualTo(200);
        assertThat(userRepository.findByEmail("unlock@example.com").orElseThrow().getFailedLoginAttempts()).isZero();
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void corsPreflight_allowsConfiguredOrigin_only() throws Exception {
        mockMvc.perform(options("/api/v1/tasks")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/api/v1/tasks")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
