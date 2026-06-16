package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for assigning a patient to a medical professional.
 *
 * Replaces the previous query-string parameters so that identifiers are not
 * leaked in access logs / browser history, matching the frontend contract
 * (POST /api/relationships/assign with a JSON body).
 */
@Data
public class AssignRelationshipRequest {

    @NotNull(message = "El identificador del paciente es obligatorio")
    private Long patientId;

    @NotNull(message = "El identificador del médico es obligatorio")
    private Long medicoId;
}
