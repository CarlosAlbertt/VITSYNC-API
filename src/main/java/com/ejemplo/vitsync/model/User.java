package com.ejemplo.vitsync.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password; // Se guardará encriptada

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.PACIENTE; // Por defecto PACIENTE

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // Métodos de UserDetails (requeridos por Spring Security)

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Retorna los roles/permisos del usuario
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Puedes implementar lógica de expiración
    }

    @Override
    public boolean isAccountNonLocked() {
        return activo; // Si está activo, no está bloqueado
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Puedes implementar lógica de expiración de contraseña
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}

// Enum para los roles
enum Role {
    ADMIN,
    PACIENTE,
    PROFESIONAL
}