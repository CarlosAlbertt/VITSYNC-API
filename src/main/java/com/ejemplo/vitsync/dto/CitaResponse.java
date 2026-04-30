package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.model.Cita;
import com.ejemplo.vitsync.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

/**
 * DTO de respuesta para Cita.
 * Mapea los campos del backend al formato que espera el frontend Vue.js.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CitaResponse {

    private Long id;

    /** Fecha en formato yyyy-MM-dd (ej: "2026-05-12") */
    private String date;

    /** Hora en formato HH:mm (ej: "10:30") */
    private String time;

    /** Nombre completo del médico (ej: "Carlos Rodríguez López") */
    private String doctor;

    /** Nombre de la especialidad del médico */
    private String specialty;

    /** Tipo: "Telemedicina" | "Presencial" */
    private String type;

    /** Localización para citas presenciales, null para telemedicina */
    private String location;

    /**
     * Estado capitalizado: "Confirmada" | "Programada" | "Completada" | "Cancelada"
     * (el frontend filtra usando estos valores exactos)
     */
    private String status;

    /** Enlace a la videoconsulta para citas de Telemedicina */
    private String videoLink;

    /** ID del paciente, útil para filtros del frontend */
    private Long pacienteId;

    /** ID del médico */
    private Long medicoId;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Convierte la entidad Cita + datos del médico al DTO que espera el frontend.
     *
     * @param cita     entidad Cita
     * @param medico   User con rol MEDICO (puede ser null si no se encuentra)
     * @param especialidadNombre nombre de la especialidad (puede ser null)
     */
    public static CitaResponse fromEntity(Cita cita, User medico, String especialidadNombre) {
        String doctorName = buildDoctorName(medico);
        String capitalizedEstado = capitalizeEstado(cita.getEstado());

        return CitaResponse.builder()
                .id(cita.getId())
                .date(cita.getFechaHora() != null ? cita.getFechaHora().format(DATE_FMT) : null)
                .time(cita.getFechaHora() != null ? cita.getFechaHora().format(TIME_FMT) : null)
                .doctor(doctorName)
                .specialty(especialidadNombre != null ? especialidadNombre : "General")
                .type(cita.getTipo())
                .location(buildLocation(cita))
                .status(capitalizedEstado)
                .videoLink(cita.getEnlaceVideoconsulta())
                .pacienteId(cita.getPacienteId())
                .medicoId(cita.getMedicoId())
                .build();
    }

    /** Overload sin especialidad */
    public static CitaResponse fromEntity(Cita cita, User medico) {
        return fromEntity(cita, medico, null);
    }

    private static String buildDoctorName(User medico) {
        if (medico == null) return "Médico desconocido";
        // Formato: "Dr. Nombre Apellido1 Apellido2"
        StringBuilder sb = new StringBuilder("Dr. ");
        if (medico.getFirstName() != null) sb.append(medico.getFirstName()).append(" ");
        if (medico.getName() != null) sb.append(medico.getName());
        return sb.toString().trim();
    }

    private static String buildLocation(Cita cita) {
        if ("Presencial".equalsIgnoreCase(cita.getTipo())) {
            return "Consulta asignada";  // Podría enriquecerse si hubiera campo ubicación en la entidad
        }
        return null;
    }

    /**
     * Convierte el estado de MAYÚSCULAS (BD) a Capitalizado (front).
     * CONFIRMADA → Confirmada, COMPLETADA → Completada, etc.
     */
    private static String capitalizeEstado(String estado) {
        if (estado == null) return "Desconocido";
        return switch (estado.toUpperCase()) {
            case "CONFIRMADA"  -> "Confirmada";
            case "PROGRAMADA"  -> "Programada";
            case "COMPLETADA"  -> "Completada";
            case "CANCELADA"   -> "Cancelada";
            case "PENDIENTE"   -> "Pendiente";
            default            -> estado.substring(0, 1).toUpperCase() + estado.substring(1).toLowerCase();
        };
    }
}
