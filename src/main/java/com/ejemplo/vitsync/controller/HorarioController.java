package com.ejemplo.vitsync.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/horarios")
@CrossOrigin(origins = "*")
public class HorarioController {

    @GetMapping
    public List<String> getHorariosDisponibles(@RequestParam(required = false) String medicoId, @RequestParam(required = false) String fecha) {
        // En una implementación real, aquí buscaríamos los horarios libres del médico en la BBDD
        // Para que la UI pueda avanzar, devolvemos unas horas mockeadas
        return Arrays.asList("09:00", "09:30", "10:00", "11:30", "12:00", "16:00", "17:30");
    }
}
