package com.james.taskflow.auth;

import com.james.taskflow.auth.token.RefreshTokenRepository;
import com.james.taskflow.auth.token.RefreshTokenService;
import com.james.taskflow.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class RefreshTokenIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void clean() { userRepository.deleteAll(); }   // refresh tokens go with their users (ON DELETE CASCADE)

    /** Registers a user, logs in, returns {accessToken, refreshToken}. */
    private String[] registerAndLogin(String email) throws Exception {
        String creds = "{\"email\":\"%s\",\"password\":\"correct-horse-battery\"}".formatted(email);
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new String[]{ JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken") };
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));
    }

    @Test
    void refresh_rotates_returnsNewTokens_andNewAccessTokenWorks() throws Exception {
        String[] t1 = registerAndLogin("rot@example.com");

        String body = refresh(t1[1]).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newAccess = JsonPath.read(body, "$.accessToken");
        String newRefresh = JsonPath.read(body, "$.refreshToken");

        assertThat(newRefresh).isNotEqualTo(t1[1]);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("rot@example.com"));
    }

    @Test
    void usedRefreshToken_cannotBeUsedAgain() throws Exception {
        String[] t1 = registerAndLogin("once@example.com");
        refresh(t1[1]).andExpect(status().isOk());
        refresh(t1[1]).andExpect(status().isUnauthorized());
    }

    @Test
    void reuseOfAnOldToken_revokesTheWholeFamily() throws Exception {
        String[] t1 = registerAndLogin("theft@example.com");

        // legitimate refresh: RT1 -> RT2
        String body = refresh(t1[1]).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String rt2 = JsonPath.read(body, "$.refreshToken");

        // a thief replays the stolen RT1: detected
        refresh(t1[1]).andExpect(status().isUnauthorized());

        // the legitimate RT2 is now dead too (proves the revocation was committed, not rolled back)
        refresh(rt2).andExpect(status().isUnauthorized());
    }

    @Test
    void database_neverStoresTheRawToken() throws Exception {
        String[] t1 = registerAndLogin("hash@example.com");

        assertThat(refreshTokenRepository.findByTokenHash(t1[1])).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(t1[1]))).isPresent();
    }
}