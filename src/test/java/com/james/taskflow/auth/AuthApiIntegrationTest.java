package com.james.taskflow.auth;

import com.james.taskflow.user.User;
import com.james.taskflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc   // let IntelliJ suggest the Boot 4 import, as before
@Testcontainers
class AuthApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    private static String body(String email, String password) {
        return """
               {"email":"%s","password":"%s"}
               """.formatted(email, password);
    }

    @Test
    void register_returns201_normalisesEmail_andNeverExposesSecrets() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("James@Example.com", "correct-horse-battery")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("james@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_storesAHash_neverThePlaintext() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("a@example.com", "correct-horse-battery")))
                .andExpect(status().isCreated());

        User saved = userRepository.findByEmail("a@example.com").orElseThrow();

        assertThat(saved.getPasswordHash()).isNotEqualTo("correct-horse-battery");
        assertThat(saved.getPasswordHash()).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches("correct-horse-battery", saved.getPasswordHash())).isTrue();
    }

    @Test
    void register_duplicateEmail_ignoringCase_returns409() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("dup@example.com", "correct-horse-battery")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("DUP@EXAMPLE.COM", "another-long-password")))
                .andExpect(status().isConflict());
    }
}