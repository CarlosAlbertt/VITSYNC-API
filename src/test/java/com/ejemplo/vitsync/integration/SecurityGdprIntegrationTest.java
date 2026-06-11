package com.ejemplo.vitsync.integration;

import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import com.ejemplo.vitsync.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for token lifecycle (refresh rotation, logout, validate),
 * role-based access control, IDOR protection and the GDPR endpoints, with the
 * full Spring context and H2.
 *
 * <p>{@link EmailService} is mocked: erasure requests trigger notification
 * emails and tests must never call the external Resend API.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Seguridad y GDPR — integration")
class SecurityGdprIntegrationTest {

    private static final String PASSWORD = "Password123!Abc";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockBean private EmailService emailService;

    private Paciente paciente;
    private User admin;

    @BeforeEach
    void setUp() {
        // Usuarios verificados insertados directamente: el flujo register+verify
        // ya está cubierto en AuthControllerIntegrationTest
        paciente = (Paciente) userRepository.findByNif("11111111H")
                .orElseGet(() -> userRepository.save(buildPaciente("11111111H", "pac@itest.es")));
        admin = userRepository.findByNif("22222222J")
                .orElseGet(() -> userRepository.save(buildAdmin("22222222J", "adm@itest.es")));
    }

    private Paciente buildPaciente(String nif, String email) {
        Paciente p = new Paciente();
        fillCommon(p, nif, email, Role.PACIENTE);
        return p;
    }

    private User buildAdmin(String nif, String email) {
        User u = new User();
        fillCommon(u, nif, email, Role.ADMIN);
        return u;
    }

    private void fillCommon(User u, String nif, String email, Role role) {
        // La entidad valida @NotBlank/@Column(nullable=false) al persistir:
        // hay que rellenar todos los campos obligatorios
        u.setName("Test");
        u.setFirstName("Integracion");
        u.setSecondName("Seguridad");
        u.setNif(nif);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setGender(Gender.HOMBRE);
        u.setRole(role);
        u.setBirthDate(LocalDate.of(1990, 1, 15));
        u.setPhone("612345678");
        u.setAddress("Calle Test 1");
        u.setPostCode("46001");
        u.setCountry("España");
        u.setVerified(true);
    }

    /** Logs in through the real endpoint and returns the parsed response. */
    private JsonNode login(String nif, String ip) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nif", nif, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    // ---- Refresh + rotación ----

    @Test
    @DisplayName("POST /refresh con token válido → 200 y nuevo par de tokens")
    void refresh_validToken_returnsNewPair() throws Exception {
        JsonNode auth = login("11111111H", "10.1.0.1");

        mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Forwarded-For", "10.1.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", auth.get("refreshToken").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /refresh reutilizando token rotado (replay) → 400")
    void refresh_replayedToken_isRejected() throws Exception {
        JsonNode auth = login("11111111H", "10.1.0.2");
        String original = auth.get("refreshToken").asText();
        String body = objectMapper.writeValueAsString(Map.of("refreshToken", original));

        // Primera rotación: OK
        mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Forwarded-For", "10.1.0.2")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        // Replay del token ya revocado: rechazado sin detalle del motivo
        mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Forwarded-For", "10.1.0.2")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /login setea cookie refresh_token httpOnly/Secure/SameSite=None")
    void login_setsHttpOnlyRefreshCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.1.0.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nif", "11111111H", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        org.junit.jupiter.api.Assertions.assertNotNull(setCookie);
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.startsWith("refresh_token="));
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("HttpOnly"));
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("Secure"));
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("SameSite=None"));
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("Path=/api/auth"));
    }

    @Test
    @DisplayName("POST /refresh solo con cookie (sin body) → 200 y cookie rotada")
    void refresh_withCookieOnly_returns200AndRotates() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.1.0.11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nif", "11111111H", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        jakarta.servlet.http.Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Forwarded-For", "10.1.0.11")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        // Rotación: la cookie devuelta lleva un token distinto al presentado
        jakarta.servlet.http.Cookie rotated = refreshResult.getResponse().getCookie("refresh_token");
        org.junit.jupiter.api.Assertions.assertNotNull(rotated);
        org.junit.jupiter.api.Assertions.assertNotEquals(refreshCookie.getValue(), rotated.getValue());
    }

    // ---- Logout ----

    @Test
    @DisplayName("POST /logout revoca el refresh token: el refresh posterior falla")
    void logout_revokesRefreshToken() throws Exception {
        JsonNode auth = login("11111111H", "10.1.0.3");
        String body = objectMapper.writeValueAsString(
                Map.of("refreshToken", auth.get("refreshToken").asText()));

        mockMvc.perform(post("/api/auth/logout")
                        .header("X-Forwarded-For", "10.1.0.3")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Forwarded-For", "10.1.0.3")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ---- Validate ----

    @Test
    @DisplayName("GET /validate sin token → 401")
    void validate_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                        .header("X-Forwarded-For", "10.1.0.4"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @DisplayName("GET /validate con token válido → 200 con nif y rol")
    void validate_withValidToken_returns200() throws Exception {
        JsonNode auth = login("11111111H", "10.1.0.5");

        mockMvc.perform(get("/api/auth/validate")
                        .header("X-Forwarded-For", "10.1.0.5")
                        .header("Authorization", "Bearer " + auth.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.nif").value("11111111H"))
                .andExpect(jsonPath("$.role").value("PACIENTE"));
    }

    // ---- Control de acceso por rol ----

    @Test
    @DisplayName("GET /api/usuarios siendo PACIENTE → 403")
    void adminEndpoint_asPaciente_returns403() throws Exception {
        JsonNode auth = login("11111111H", "10.1.0.6");

        mockMvc.perform(get("/api/usuarios")
                        .header("X-Forwarded-For", "10.1.0.6")
                        .header("Authorization", "Bearer " + auth.get("token").asText()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/usuarios siendo ADMIN → 200")
    void adminEndpoint_asAdmin_returns200() throws Exception {
        JsonNode auth = login("22222222J", "10.1.0.7");

        mockMvc.perform(get("/api/usuarios")
                        .header("X-Forwarded-For", "10.1.0.7")
                        .header("Authorization", "Bearer " + auth.get("token").asText()))
                .andExpect(status().isOk());
    }

    // ---- IDOR ----

    @Test
    @DisplayName("GET /VitSync-app/{id} de OTRO usuario → 403 (IDOR)")
    void userById_otherUser_returns403() throws Exception {
        JsonNode auth = login("11111111H", "10.1.0.8");

        mockMvc.perform(get("/VitSync-app/" + admin.getId())
                        .header("X-Forwarded-For", "10.1.0.8")
                        .header("Authorization", "Bearer " + auth.get("token").asText()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /VitSync-app/{id} propio → 200 sin password ni verificationCode")
    void userById_self_returns200WithoutSecrets() throws Exception {
        JsonNode auth = login("11111111H", "10.1.0.9");

        mockMvc.perform(get("/VitSync-app/" + paciente.getId())
                        .header("X-Forwarded-For", "10.1.0.9")
                        .header("Authorization", "Bearer " + auth.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nif").value("11111111H"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.verificationCode").doesNotExist());
    }

    // ---- GDPR ----

    @Test
    @DisplayName("GET /my-data propio → 200 con estructura completa")
    void myData_self_returnsFullStructure() throws Exception {
        JsonNode auth = login("11111111H", "10.1.1.1");

        mockMvc.perform(get("/api/users/" + paciente.getId() + "/my-data")
                        .header("X-Forwarded-For", "10.1.1.1")
                        .header("Authorization", "Bearer " + auth.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil.nif").value("11111111H"))
                .andExpect(jsonPath("$.citas").isArray())
                .andExpect(jsonPath("$.informes").isArray())
                .andExpect(jsonPath("$.mensajes").isArray());
    }

    @Test
    @DisplayName("GET /my-data de OTRO usuario → 403")
    void myData_otherUser_returns403() throws Exception {
        JsonNode auth = login("11111111H", "10.1.1.2");

        mockMvc.perform(get("/api/users/" + admin.getId() + "/my-data")
                        .header("X-Forwarded-For", "10.1.1.2")
                        .header("Authorization", "Bearer " + auth.get("token").asText()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /gdpr-delete propio → 202 con fecha de anonimización")
    void gdprDelete_self_returns202WithScheduledDate() throws Exception {
        // Usuario dedicado: la solicitud suspende la cuenta y rompería otros tests
        Paciente borrable = userRepository.save(buildPaciente("33333333P", "del@itest.es"));
        JsonNode auth = login("33333333P", "10.1.1.3");

        mockMvc.perform(delete("/api/users/" + borrable.getId() + "/gdpr-delete")
                        .header("X-Forwarded-For", "10.1.1.3")
                        .header("Authorization", "Bearer " + auth.get("token").asText()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.scheduledAnonymizationDate").isNotEmpty());

        // La cuenta queda suspendida: el login posterior debe fallar
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.1.1.4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nif", "33333333P", "password", PASSWORD))))
                .andExpect(status().is4xxClientError());
    }
}
