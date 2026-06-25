package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.EnfermedadRequest;
import com.ejemplo.vitsync.exception.ResourceNotFoundException;
import com.ejemplo.vitsync.model.Enfermedad;
import com.ejemplo.vitsync.repository.EnfermedadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EnfermedadService} (CRUD del catálogo de enfermedades).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnfermedadService — catálogo de enfermedades")
class EnfermedadServiceTest {

    @Mock private EnfermedadRepository enfermedadRepository;
    @InjectMocks private EnfermedadService service;

    private EnfermedadRequest request;

    @BeforeEach
    void setUp() {
        request = new EnfermedadRequest();
        request.setNombre("  Hipertensión  ");
        request.setDescripcion("  Tensión alta  ");
        request.setEspecialidadRelacionada("  Cardiología ");
        request.setTratamientos(List.of("Dieta", "Medicación"));
    }

    @Test
    @DisplayName("create: normaliza campos, activa por defecto y persiste")
    void create_ok() {
        when(enfermedadRepository.save(any(Enfermedad.class))).thenAnswer(i -> i.getArgument(0));

        Enfermedad e = service.create(request);

        assertEquals("Hipertensión", e.getNombre());
        assertEquals("Tensión alta", e.getDescripcion());
        assertEquals("Cardiología", e.getEspecialidadRelacionada());
        assertEquals(List.of("Dieta", "Medicación"), e.getTratamientos());
        assertTrue(e.getActivo());
        verify(enfermedadRepository).save(e);
    }

    @Test
    @DisplayName("create: respeta activo=false si se envía")
    void create_inactiva() {
        request.setActivo(false);
        when(enfermedadRepository.save(any(Enfermedad.class))).thenAnswer(i -> i.getArgument(0));

        assertFalse(service.create(request).getActivo());
    }

    @Test
    @DisplayName("update: aplica cambios sobre la enfermedad existente")
    void update_ok() {
        Enfermedad existente = new Enfermedad();
        existente.setId(5L);
        existente.setActivo(true);
        when(enfermedadRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(enfermedadRepository.save(any(Enfermedad.class))).thenAnswer(i -> i.getArgument(0));

        Enfermedad e = service.update(5L, request);

        assertEquals("Hipertensión", e.getNombre());
        assertEquals(2, e.getTratamientos().size());
    }

    @Test
    @DisplayName("findById: 404 si no existe")
    void findById_notFound() {
        when(enfermedadRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    @DisplayName("toggleActivo: invierte el estado")
    void toggle_ok() {
        Enfermedad e = new Enfermedad();
        e.setId(1L);
        e.setActivo(true);
        when(enfermedadRepository.findById(1L)).thenReturn(Optional.of(e));
        when(enfermedadRepository.save(any(Enfermedad.class))).thenAnswer(i -> i.getArgument(0));

        assertFalse(service.toggleActivo(1L).getActivo());
    }

    @Test
    @DisplayName("findAllActive delega en el repositorio")
    void findAllActive_ok() {
        when(enfermedadRepository.findByActivoTrue()).thenReturn(List.of(new Enfermedad()));
        assertEquals(1, service.findAllActive().size());
        verify(enfermedadRepository).findByActivoTrue();
    }

    @Test
    @DisplayName("delete elimina la enfermedad encontrada")
    void delete_ok() {
        Enfermedad e = new Enfermedad();
        e.setId(3L);
        when(enfermedadRepository.findById(3L)).thenReturn(Optional.of(e));

        service.delete(3L);

        verify(enfermedadRepository).delete(e);
    }
}
