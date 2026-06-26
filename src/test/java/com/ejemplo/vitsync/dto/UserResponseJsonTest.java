package com.ejemplo.vitsync.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica que el estado de verificación se serializa como "isVerified" (no "verified"). */
class UserResponseJsonTest {

    @Test
    void serializaComoIsVerified() throws Exception {
        UserResponse r = UserResponse.builder().id(1L).isVerified(true).build();
        String json = new ObjectMapper().writeValueAsString(r);
        System.out.println("JSON => " + json);
        assertTrue(json.contains("\"isVerified\":true"), "Debe exponer isVerified. JSON: " + json);
        assertFalse(json.contains("\"verified\""), "No debe exponer 'verified'. JSON: " + json);
    }
}
