package com.ejemplo.vitsync.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Data
public class CitaRequest {
    private Map<String, Object> location;
    private String specialty;
    private Map<String, Object> doctor;
    private String date; // Puede venir como string "2026-05-10T00:00:00.000Z"
    private String time;
    private String reason;
}
