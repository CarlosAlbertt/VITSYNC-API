package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.model.Medico;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MedicoResponse {

    private Long id;
    // Datos heredados de User
    private String name;
    private String firstName;
    private String secondName;
    private String nif;
    private String email;
    private String birthDate;
    private String phone;
    private String address;
    private String postCode;
    private String country;
    private boolean isVerified;
    // Datos específicos de Médico
    private String numeroColegiado;
    private String fotoUrl;
    private String bio;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Especialidad resumida
    private EspecialidadSimple especialidad;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EspecialidadSimple {
        private Long id;
        private String nombre;
        private String codigo;
        private String tipo;
    }

    public static MedicoResponse fromEntity(Medico medico) {
        EspecialidadSimple espSimple = null;
        if (medico.getEspecialidad() != null) {
            espSimple = EspecialidadSimple.builder()
                    .id(medico.getEspecialidad().getId())
                    .nombre(medico.getEspecialidad().getNombre())
                    .codigo(medico.getEspecialidad().getCodigo())
                    .tipo(medico.getEspecialidad().getTipo())
                    .build();
        }

        return MedicoResponse.builder()
                .id(medico.getId())
                .name(medico.getName())
                .firstName(medico.getFirstName())
                .secondName(medico.getSecondName())
                .nif(medico.getNif())
                .email(medico.getEmail())
                .birthDate(medico.getBirthDate())
                .phone(medico.getPhone())
                .address(medico.getAddress())
                .postCode(medico.getPostCode())
                .country(medico.getCountry())
                .isVerified(medico.isVerified())
                .numeroColegiado(medico.getNumeroColegiado())
                .fotoUrl(medico.getFotoUrl())
                .bio(medico.getBio())
                .activo(medico.getActivo())
                .createdAt(medico.getCreatedAt())
                .updatedAt(medico.getUpdatedAt())
                .especialidad(espSimple)
                .build();
    }
}
