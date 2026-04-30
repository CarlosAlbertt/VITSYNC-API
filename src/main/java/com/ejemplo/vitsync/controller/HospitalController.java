package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.model.Hospital;
import com.ejemplo.vitsync.repository.HospitalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/hospitales")
@CrossOrigin(origins = "*")
public class HospitalController {

    @Autowired
    private HospitalRepository hospitalRepository;

    @GetMapping
    public List<Hospital> getAllHospitales() {
        List<Hospital> dbHospitales = hospitalRepository.findAll();
        
        // Si la base de datos está vacía, devolvemos algunos hospitales de prueba para que la UI funcione
        if (dbHospitales == null || dbHospitales.isEmpty()) {
            return Arrays.asList(
                new Hospital(1L, "VitSync Centro Médico Madrid", "Calle Principal 123, Madrid", "https://images.unsplash.com/photo-1587351021759-3e566b6af7cc?q=80&w=300&auto=format&fit=crop", "+34 912 345 678"),
                new Hospital(2L, "VitSync Clínica Barcelona", "Av. Diagonal 456, Barcelona", "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?q=80&w=300&auto=format&fit=crop", "+34 931 234 567"),
                new Hospital(3L, "VitSync Salud Valencia", "Gran Vía 789, Valencia", "https://images.unsplash.com/photo-1538108149393-cebb47cdf141?q=80&w=300&auto=format&fit=crop", "+34 961 112 223")
            );
        }
        
        return dbHospitales;
    }
}
