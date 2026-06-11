package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.EspecialidadRequest;
import com.ejemplo.vitsync.dto.MedicoRequest;
import com.ejemplo.vitsync.dto.UserUpdateRequest;
import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.exception.ResourceNotFoundException;
import com.ejemplo.vitsync.model.Especialidad;
import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.EspecialidadRepository;
import com.ejemplo.vitsync.repository.MedicoRepository;
import com.ejemplo.vitsync.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminUserService, MedicoService and EspecialidadService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUser / Medico / Especialidad services")
class AdminMedicoEspecialidadServiceTest {

    @Nested
    @DisplayName("AdminUserService")
    class AdminUserServiceTest {
        @Mock UserRepository userRepository;
        @Mock PasswordEncoder passwordEncoder;
        @InjectMocks AdminUserService service;

        private UserUpdateRequest req() {
            return UserUpdateRequest.builder()
                    .name("N").firstName("A").secondName("B").nif("12345678Z")
                    .email("e@e.es").gender(Gender.HOMBRE).role(Role.PACIENTE)
                    .birthDate(LocalDate.of(1990, 1, 1)).phone("612345678")
                    .address("dir").postCode("46001").country("ES").build();
        }

        @Test
        void findAll_paged() {
            when(userRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(new User())));
            assertEquals(1, service.findAll(Pageable.unpaged()).getTotalElements());
        }

        @Test
        void findByRole_paged() {
            when(userRepository.findByRole(eq(Role.MEDICO), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(new User())));
            assertEquals(1, service.findByRole(Role.MEDICO, Pageable.unpaged()).getTotalElements());
        }

        @Test
        void findById_delegates() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
            assertTrue(service.findById(1L).isPresent());
        }

        @Test
        void update_notFound_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.update(1L, req()));
        }

        @Test
        void update_duplicateEmail_throwsConflict() {
            User existing = new User(); existing.setId(1L);
            User other = new User(); other.setId(2L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.findByEmail("e@e.es")).thenReturn(Optional.of(other));
            assertThrows(DataIntegrityViolationException.class, () -> service.update(1L, req()));
        }

        @Test
        void update_valid_savesAndOptionallyHashesPassword() {
            User existing = new User(); existing.setId(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.findByEmail("e@e.es")).thenReturn(Optional.empty());
            when(userRepository.findByNif("12345678Z")).thenReturn(Optional.empty());
            when(userRepository.save(existing)).thenReturn(existing);

            UserUpdateRequest r = req();
            r.setPassword("Password123!Abc");
            when(passwordEncoder.encode("Password123!Abc")).thenReturn("HASH");

            service.update(1L, r);
            assertEquals("HASH", existing.getPassword());
            verify(userRepository).save(existing);
        }

        @Test
        void delete_notFound_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.delete(1L));
        }

        @Test
        void delete_existing_deletes() {
            User u = new User();
            when(userRepository.findById(1L)).thenReturn(Optional.of(u));
            service.delete(1L);
            verify(userRepository).delete(u);
        }

        @Test
        void setVerified_setsFlag() {
            User u = new User();
            when(userRepository.findById(1L)).thenReturn(Optional.of(u));
            when(userRepository.save(u)).thenReturn(u);
            service.setVerified(1L, true);
            assertTrue(u.isVerified());
        }
    }

    @Nested
    @DisplayName("MedicoService")
    class MedicoServiceTest {
        @Mock MedicoRepository medicoRepository;
        @Mock EspecialidadRepository especialidadRepository;
        @Mock PasswordEncoder passwordEncoder;
        @InjectMocks MedicoService service;

        private MedicoRequest req() {
            MedicoRequest r = new MedicoRequest();
            r.setName("Dr"); r.setFirstName("A"); r.setSecondName("B");
            r.setNif("12345678Z"); r.setEmail("d@d.es"); r.setGender(Gender.HOMBRE);
            r.setBirthDate(LocalDate.of(1980, 1, 1)); r.setPhone("612345678");
            r.setAddress("dir"); r.setPostCode("46001"); r.setCountry("ES");
            r.setNumeroColegiado("COL-1"); r.setPassword("Password123!Abc");
            return r;
        }

        @Test
        void create_duplicateColegiado_throws() {
            when(medicoRepository.existsByNumeroColegiado("COL-1")).thenReturn(true);
            assertThrows(IllegalArgumentException.class, () -> service.create(req()));
        }

        @Test
        void create_noPassword_throws() {
            MedicoRequest r = req(); r.setPassword(" ");
            when(medicoRepository.existsByNumeroColegiado("COL-1")).thenReturn(false);
            assertThrows(IllegalArgumentException.class, () -> service.create(r));
        }

        @Test
        void create_valid_savesWithRoleMedico() {
            when(medicoRepository.existsByNumeroColegiado("COL-1")).thenReturn(false);
            when(passwordEncoder.encode("Password123!Abc")).thenReturn("HASH");
            when(medicoRepository.save(any(Medico.class))).thenAnswer(i -> i.getArgument(0));
            Medico m = service.create(req());
            assertEquals(Role.MEDICO, m.getRole());
            assertEquals("HASH", m.getPassword());
        }

        @Test
        void update_notFound_throws() {
            when(medicoRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class, () -> service.update(1L, req()));
        }

        @Test
        void update_withEspecialidad_resolvesIt() {
            Medico m = new Medico();
            when(medicoRepository.findById(1L)).thenReturn(Optional.of(m));
            when(medicoRepository.existsByNumeroColegiadoAndIdNot("COL-1", 1L)).thenReturn(false);
            when(medicoRepository.save(m)).thenReturn(m);
            MedicoRequest r = req(); r.setEspecialidadId(7L); r.setPassword(null);
            when(especialidadRepository.findById(7L)).thenReturn(Optional.of(new Especialidad()));
            service.update(1L, r);
            assertNotNull(m.getEspecialidad());
        }

        @Test
        void toggleActivo_flips() {
            Medico m = new Medico(); m.setActivo(true);
            when(medicoRepository.findById(1L)).thenReturn(Optional.of(m));
            when(medicoRepository.save(m)).thenReturn(m);
            service.toggleActivo(1L);
            assertFalse(m.getActivo());
        }

        @Test
        void delete_existing_deletes() {
            Medico m = new Medico();
            when(medicoRepository.findById(1L)).thenReturn(Optional.of(m));
            service.delete(1L);
            verify(medicoRepository).delete(m);
        }

        @Test
        void reads_delegate() {
            when(medicoRepository.findAllActiveWithEspecialidad()).thenReturn(List.of(new Medico()));
            when(medicoRepository.findAllWithEspecialidad()).thenReturn(List.of(new Medico()));
            when(medicoRepository.findByIdWithEspecialidad(1L)).thenReturn(Optional.of(new Medico()));
            when(medicoRepository.findByEspecialidadId(2L)).thenReturn(List.of(new Medico()));
            assertEquals(1, service.findAllActive().size());
            assertEquals(1, service.findAll().size());
            assertTrue(service.findById(1L).isPresent());
            assertEquals(1, service.findByEspecialidad(2L).size());
        }
    }

    @Nested
    @DisplayName("EspecialidadService")
    class EspecialidadServiceTest {
        @Mock EspecialidadRepository repo;
        @InjectMocks EspecialidadService service;

        private EspecialidadRequest req() {
            EspecialidadRequest r = new EspecialidadRequest();
            r.setNombre("Cardiología"); r.setCodigo("CARD"); r.setTipo("medica");
            r.setDescripcion("desc");
            return r;
        }

        @Test
        void create_duplicateCodigo_throws() {
            when(repo.existsByCodigo("CARD")).thenReturn(true);
            assertThrows(IllegalArgumentException.class, () -> service.create(req()));
        }

        @Test
        void create_valid_generatesSlugAndUppercasesTipo() {
            when(repo.existsByCodigo("CARD")).thenReturn(false);
            when(repo.existsBySlug("cardiologia")).thenReturn(false);
            when(repo.save(any(Especialidad.class))).thenAnswer(i -> i.getArgument(0));
            Especialidad e = service.create(req());
            assertEquals("cardiologia", e.getSlug());
            assertEquals("MEDICA", e.getTipo());
        }

        @Test
        void update_notFound_throws() {
            when(repo.findById(1L)).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class, () -> service.update(1L, req()));
        }

        @Test
        void update_valid_saves() {
            Especialidad e = new Especialidad();
            when(repo.findById(1L)).thenReturn(Optional.of(e));
            when(repo.existsByCodigoAndIdNot("CARD", 1L)).thenReturn(false);
            when(repo.existsBySlugAndIdNot("cardiologia", 1L)).thenReturn(false);
            when(repo.save(e)).thenReturn(e);
            service.update(1L, req());
            assertEquals("CARD", e.getCodigo());
        }

        @Test
        void toggleActivo_flips() {
            Especialidad e = new Especialidad(); e.setActivo(true);
            when(repo.findById(1L)).thenReturn(Optional.of(e));
            when(repo.save(e)).thenReturn(e);
            service.toggleActivo(1L);
            assertFalse(e.getActivo());
        }

        @Test
        void delete_existing_deletes() {
            Especialidad e = new Especialidad();
            when(repo.findById(1L)).thenReturn(Optional.of(e));
            service.delete(1L);
            verify(repo).delete(e);
        }

        @Test
        void reads_delegate() {
            when(repo.findAllActiveWithMedicos()).thenReturn(List.of(new Especialidad()));
            when(repo.findAll()).thenReturn(List.of(new Especialidad()));
            when(repo.findByTipoAndActivoTrue("MEDICA")).thenReturn(List.of(new Especialidad()));
            assertEquals(1, service.findAllActive().size());
            assertEquals(1, service.findAll().size());
            assertEquals(1, service.findByTipo("MEDICA").size());
        }
    }
}
