package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.model.Enfermedad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnfermedadResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private List<String> tratamientos;
    private String especialidadRelacionada;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EnfermedadResponse fromEntity(Enfermedad enfermedad) {
        return EnfermedadResponse.builder()
                .id(enfermedad.getId())
                .nombre(enfermedad.getNombre())
                .descripcion(enfermedad.getDescripcion())
                .tratamientos(enfermedad.getTratamientos())
                .especialidadRelacionada(enfermedad.getEspecialidadRelacionada())
                .activo(enfermedad.getActivo())
                .createdAt(enfermedad.getCreatedAt())
                .updatedAt(enfermedad.getUpdatedAt())
                .build();
    }
}
