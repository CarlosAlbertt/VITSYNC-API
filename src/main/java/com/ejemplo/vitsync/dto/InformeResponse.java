package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.model.Informe;
import com.ejemplo.vitsync.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para Informe.
 * Mapea los campos del backend al formato que espera el frontend Vue.js.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InformeResponse {

    private Long id;

    /** título del informe (alias de "titulo" en la entidad) */
    private String title;

    /** fecha en formato yyyy-MM-dd (alias de "fecha") */
    private String date;

    /** tipo de informe (alias de "tipo") */
    private String type;

    /** especialidad del médico que emitió el informe */
    private String specialty;

    /** nombre del médico en formato "Dr. Nombre Apellido" */
    private String doctor;

    /**
     * Estado legible para el frontend.
     * Si el informe tiene archivo → "Disponible", si no → "Pendiente".
     */
    private String status;

    /** notas personales del paciente */
    private String notes;

    /** si está marcado como favorito */
    private boolean favorite;

    /** URL del archivo PDF (puede ser null) */
    private String archivoUrl;

    /** ID del paciente */
    private Long pacienteId;

    /** ID del médico */
    private Long medicoId;

    /**
     * Convierte la entidad Informe + datos del médico al DTO que espera el frontend.
     *
     * @param informe         entidad Informe
     * @param medico          User con rol MEDICO (puede ser null)
     * @param especialidadNombre nombre de la especialidad (puede ser null)
     */
    public static InformeResponse fromEntity(Informe informe, User medico, String especialidadNombre) {
        String doctorName = buildDoctorName(medico);
        String computedStatus = informe.getArchivoUrl() != null ? "Disponible" : "Pendiente";

        return InformeResponse.builder()
                .id(informe.getId())
                .title(informe.getTitulo())
                .date(informe.getFecha() != null ? informe.getFecha().toString() : null)
                .type(informe.getTipo())
                .specialty(especialidadNombre != null ? especialidadNombre : "General")
                .doctor(doctorName)
                .status(computedStatus)
                .notes(informe.getNotasPersonales())
                .favorite(informe.isFavorito())
                .archivoUrl(informe.getArchivoUrl())
                .pacienteId(informe.getPacienteId())
                .medicoId(informe.getMedicoId())
                .build();
    }

    /** Overload sin especialidad */
    public static InformeResponse fromEntity(Informe informe, User medico) {
        return fromEntity(informe, medico, null);
    }

    private static String buildDoctorName(User medico) {
        if (medico == null) return "Médico desconocido";
        StringBuilder sb = new StringBuilder("Dr. ");
        if (medico.getFirstName() != null) sb.append(medico.getFirstName()).append(" ");
        if (medico.getName() != null) sb.append(medico.getName());
        return sb.toString().trim();
    }
}
