package com.ejemplo.vitsync.integration;

import com.ejemplo.vitsync.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@code /api/auth} with the full Spring context and H2.
 *
 * <p>Covers validation (400), bad credentials (401), successful registration
 * (201) and the login rate limit (429 after 5 attempts).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController — integration")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("POST /login with empty body → 400 with field errors")
    void login_emptyBody_returns400() throws Exception {
        // X-Forwarded-For único por test: aísla el bucket de rate limit por IP
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @DisplayName("POST /login with unknown user → 401 generic message")
    void login_unknownUser_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("nif", "00000000T", "password", "whatever123"));
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.0.0.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("POST /register with invalid NIF → 400")
    void register_invalidNif_returns400() throws Exception {
        Map<String, Object> body = validRegisterBody();
        body.put("nif", "12345678A"); // letra de control incorrecta
        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "10.0.1.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register with valid data → 201, password stored hashed")
    void register_validData_returns201() throws Exception {
        Map<String, Object> body = validRegisterBody();
        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "10.0.1.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").doesNotExist());

        var saved = userRepository.findByNif("12345678Z").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("Password123!Abc", saved.getPassword());
    }

    @Test
    @DisplayName("POST /login 6th attempt within window → 429 with Retry-After")
    void login_rateLimited_returns429() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("nif", "99999999R", "password", "whatever123"));
        // 5 permitidos (IP dedicada para no interferir con otros tests)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .header("X-Forwarded-For", "10.0.0.99")
                    .contentType(MediaType.APPLICATION_JSON).content(body));
        }
        // 6º bloqueado
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.0.0.99")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    private Map<String, Object> validRegisterBody() {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", "Test");
        body.put("firstName", "Usuario");
        body.put("secondName", "Prueba");
        body.put("nif", "12345678Z");
        body.put("email", "itest@vitsync.es");
        body.put("password", "Password123!Abc");
        body.put("gender", "HOMBRE");
        body.put("role", "PACIENTE");
        body.put("birthDate", "1990-01-15");
        body.put("phone", "612345678");
        body.put("address", "Calle Test 123");
        body.put("postCode", "46001");
        body.put("country", "España");
        return body;
    }
}
