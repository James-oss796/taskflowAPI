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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
        String tampered = tamper(token);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgedPayload_withOriginalSignature_returns401() throws Exception {
        register("forge@example.com", "correct-horse-battery");
        String token = login("forge@example.com", "correct-horse-battery");

        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forged = payload.replace("\"USER\"", "\"ADMIN\"");
        assertThat(forged).isNotEqualTo(payload);              // make sure we really changed the roles

        String forgedToken = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(forged.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }

    private static String tamper(String token) {
        String[] parts = token.split("\\.");
        char first = parts[2].charAt(0);                       // first signature char: all 6 bits are significant
        char replacement = (first == 'A') ? 'B' : 'A';
        return parts[0] + "." + parts[1] + "." + replacement + parts[2].substring(1);
    }
}
