package com.ejemplo.vitsync.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para el GlobalExceptionHandler.
 *
 * Verifica que las excepciones de validación se traducen correctamente
 * a respuestas HTTP con los códigos y formatos esperados.
 *
 * Usa MockMvc para simular peticiones HTTP sin levantar un servidor real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GlobalExceptionHandler — Manejo de Errores HTTP")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/auth/register con body vacío → 400 con errores de validación")
    void register_withEmptyBody_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register con NIF inválido → 400 con error específico")
    void register_withInvalidNif_returns400WithNifError() throws Exception {
        String body = """
                {
                    "name": "Test",
                    "firstName": "User",
                    "secondName": "Prueba",
                    "nif": "INVALIDO",
                    "email": "test@test.com",
                    "password": "Password123",
                    "gender": "HOMBRE",
                    "role": "PACIENTE",
                    "birthDate": "1990-01-15",
                    "phone": "612345678",
                    "address": "Calle Test",
                    "postCode": "46001",
                    "country": "España"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nif").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login con credenciales vacías → 400")
    void login_withEmptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
