package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.model.Hospital;
import com.ejemplo.vitsync.repository.HospitalRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Controlador REST para la gestión de hospitales y centros médicos.
 *
 * Endpoints públicos (no requieren autenticación):
 * - GET /api/hospitales → Lista todos los hospitales
 *
 * NOTA: @CrossOrigin eliminado porque CORS se gestiona globalmente en SecurityConfig.
 */
@RestController
@RequestMapping("/api/hospitales")
public class HospitalController {

    private final HospitalRepository hospitalRepository;

    public HospitalController(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    /**
     * Obtiene todos los hospitales del sistema.
     * Si la BD está vacía, devuelve datos de ejemplo como fallback para la UI.
     *
     * @return Lista de hospitales
     */
    @GetMapping
    public List<Hospital> getAllHospitales() {
        List<Hospital> dbHospitales = hospitalRepository.findAll();

        // Fallback: Si la BD está vacía, devolver hospitales de ejemplo para la UI
        if (dbHospitales == null || dbHospitales.isEmpty()) {
            return Arrays.asList(
                new Hospital(1L, "VitSync Centro Médico Madrid", "Calle Principal 123, Madrid",
                        "https://images.unsplash.com/photo-1587351021759-3e566b6af7cc?q=80&w=300&auto=format&fit=crop",
                        "+34 912 345 678"),
                new Hospital(2L, "VitSync Clínica Barcelona", "Av. Diagonal 456, Barcelona",
                        "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?q=80&w=300&auto=format&fit=crop",
                        "+34 931 234 567"),
                new Hospital(3L, "VitSync Salud Valencia", "Gran Vía 789, Valencia",
                        "https://images.unsplash.com/photo-1538108149393-cebb47cdf141?q=80&w=300&auto=format&fit=crop",
                        "+34 961 112 223")
            );
        }

        return dbHospitales;
    }
}
