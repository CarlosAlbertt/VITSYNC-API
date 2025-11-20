package com.ejemplo.vitsync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token; // JWT token
    private String type = "Bearer"; // Tipo de token
    private Long id;
    private String username;
    private String email;
    private String nombre;
    private String role;
    private String message;
}