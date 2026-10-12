package com.james.taskflow.auth;

import com.james.taskflow.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void clean() { userRepository.deleteAll(); }

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    @Test
    void protectedEndpoint_withoutToken_returns401_withProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void fullFlow_register_login_thenCallProtectedEndpoints() throws Exception {
        register("flow@example.com", "correct-horse-battery");
        String token = login("flow@example.com", "correct-horse-battery");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        mockMvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void login_unknownEmailAndWrongPassword_areIndistinguishable() throws Exception {
        register("real@example.com", "correct-horse-battery");

        String wrongPassword = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"real@example.com\",\"password\":\"nope-nope-nope\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String unknownEmail = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@example.com\",\"password\":\"nope-nope-nope\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<String>read(wrongPassword, "$.detail"))
                .isEqualTo(JsonPath.<String>read(unknownEmail, "$.detail"));
    }

    @Test
    void tamperedToken_returns401() throws Exception {
        register("tamper@example.com", "correct-horse-battery");
        String token = login("tamper@example.com", "correct-horse-battery");
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }
}
