package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.model.Especialidad;
import com.ejemplo.vitsync.model.Medico;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EspecialidadResponse {

    private Long id;
    private String nombre;
    private String codigo;
    private String descripcion;
    private String tipo;
    private String slug;
    private String icono;
    private Boolean activo;
    private List<MedicoSimple> medicos;

    // DTO interno simplificado para médicos (evita recursión)
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MedicoSimple {
        private Long id;
        private String nombre; // nombre + apellidos
        private String numeroColegiado;
        private String bio;
    }

    // Método factory para convertir entidad a DTO
    public static EspecialidadResponse fromEntity(Especialidad especialidad) {
        List<MedicoSimple> medicosList = null;

        if (especialidad.getMedicos() != null && !especialidad.getMedicos().isEmpty()) {
            medicosList = especialidad.getMedicos().stream()
                    .filter(m -> m.getActivo())
                    .map(m -> MedicoSimple.builder()
                            .id(m.getId())
                            .nombre(m.getName() + " " + m.getFirstName() + " " + m.getSecondName())
                            .numeroColegiado(m.getNumeroColegiado())
                            .bio(m.getBio())
                            .build())
                    .collect(Collectors.toList());
        }

        return EspecialidadResponse.builder()
                .id(especialidad.getId())
                .nombre(especialidad.getNombre())
                .codigo(especialidad.getCodigo())
                .descripcion(especialidad.getDescripcion())
                .tipo(especialidad.getTipo())
                .slug(especialidad.getSlug())
                .icono(especialidad.getIcono())
                .activo(especialidad.getActivo())
                .medicos(medicosList)
                .build();
    }
}
