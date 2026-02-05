package com.ejemplo.vitsync.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "medicos")
@Entity
@PrimaryKeyJoinColumn(name = "id")
public class Medico extends User {

    @Column(name = "numero_colegiado", unique = true)
    private String numeroColegiado;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relación: Un médico pertenece a una especialidad
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id")
    @ToString.Exclude
    private Especialidad especialidad;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
