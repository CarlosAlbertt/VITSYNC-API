package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.model.Cita;
import com.ejemplo.vitsync.model.Informe;
import com.ejemplo.vitsync.model.Mensaje;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.CitaRepository;
import com.ejemplo.vitsync.repository.InformeRepository;
import com.ejemplo.vitsync.repository.MensajeRepository;
import com.ejemplo.vitsync.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the small CRUD/query services: User, Cita, Informe, Chat.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicios simples (User/Cita/Informe/Chat)")
class SimpleServicesTest {

    @Nested
    @DisplayName("UserService")
    class UserServiceTest {
        @Mock UserRepository userRepository;
        @InjectMocks UserService userService;

        @Test
        void findAll_delegatesToRepo() {
            when(userRepository.findAll()).thenReturn(List.of(new User()));
            assertEquals(1, userService.findAll().size());
        }

        @Test
        void findById_found_returnsUser() {
            User u = new User();
            when(userRepository.findById(1L)).thenReturn(Optional.of(u));
            assertSame(u, userService.findById(1L));
        }

        @Test
        void findById_notFound_returnsNull() {
            when(userRepository.findById(9L)).thenReturn(Optional.empty());
            assertNull(userService.findById(9L));
        }

        @Test
        void saveUser_delegates() {
            User u = new User();
            userService.saveUser(u);
            verify(userRepository).save(u);
        }

        @Test
        void updateAvatar_delegates() {
            when(userRepository.updateAvatarUrl(1L, "url")).thenReturn(1);
            assertEquals(1, userService.updateAvatar(1L, "url"));
        }

        @Test
        void deleteUser_delegates() {
            User u = new User();
            userService.deleteUser(u);
            verify(userRepository).delete(u);
        }
    }

    @Nested
    @DisplayName("CitaService")
    class CitaServiceTest {
        @Mock CitaRepository citaRepository;
        @InjectMocks CitaService citaService;

        @Test
        void getAllCitas_delegates() {
            when(citaRepository.findAll()).thenReturn(List.of(new Cita()));
            assertEquals(1, citaService.getAllCitas().size());
        }

        @Test
        void getCitaById_notFound_returnsNull() {
            when(citaRepository.findById(5L)).thenReturn(Optional.empty());
            assertNull(citaService.getCitaById(5L));
        }

        @Test
        void cancelCita_existing_setsCancelada() {
            Cita c = new Cita();
            c.setEstado("Programada");
            when(citaRepository.findById(1L)).thenReturn(Optional.of(c));
            citaService.cancelCita(1L);
            assertEquals("Cancelada", c.getEstado());
            verify(citaRepository).save(c);
        }

        @Test
        void cancelCita_missing_noSave() {
            when(citaRepository.findById(1L)).thenReturn(Optional.empty());
            citaService.cancelCita(1L);
            verify(citaRepository, never()).save(any());
        }

        @Test
        void saveCita_delegates() {
            Cita c = new Cita();
            when(citaRepository.save(c)).thenReturn(c);
            assertSame(c, citaService.saveCita(c));
        }

        @Test
        void getCitasByNif_delegates() {
            when(citaRepository.findByPaciente_Nif("X")).thenReturn(List.of(new Cita()));
            assertEquals(1, citaService.getCitasByNif("X").size());
        }
    }

    @Nested
    @DisplayName("InformeService")
    class InformeServiceTest {
        @Mock InformeRepository informeRepository;
        @InjectMocks InformeService informeService;

        @Test
        void getAllInformes_delegates() {
            when(informeRepository.findAll()).thenReturn(List.of(new Informe()));
            assertEquals(1, informeService.getAllInformes().size());
        }

        @Test
        void getInformeById_found() {
            Informe i = new Informe();
            when(informeRepository.findById(1L)).thenReturn(Optional.of(i));
            assertSame(i, informeService.getInformeById(1L));
        }

        @Test
        void getInformesByNif_delegates() {
            when(informeRepository.findByPaciente_Nif("N")).thenReturn(List.of(new Informe()));
            assertEquals(1, informeService.getInformesByNif("N").size());
        }

        @Test
        void updateNotas_owner_updates() {
            Paciente p = new Paciente();
            p.setNif("OWNER");
            Informe i = new Informe();
            i.setPaciente(p);
            when(informeRepository.findById(1L)).thenReturn(Optional.of(i));
            informeService.updateNotasPersonales(1L, "nota", "OWNER");
            assertEquals("nota", i.getNotasPersonales());
            verify(informeRepository).save(i);
        }

        @Test
        void updateNotas_notOwner_throwsAccessDenied() {
            Paciente p = new Paciente();
            p.setNif("OTHER");
            Informe i = new Informe();
            i.setPaciente(p);
            when(informeRepository.findById(1L)).thenReturn(Optional.of(i));
            assertThrows(AccessDeniedException.class,
                    () -> informeService.updateNotasPersonales(1L, "x", "OWNER"));
            verify(informeRepository, never()).save(any());
        }

        @Test
        void updateNotas_missing_throwsNotFound() {
            when(informeRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(com.ejemplo.vitsync.exception.ResourceNotFoundException.class,
                    () -> informeService.updateNotasPersonales(1L, "x", "OWNER"));
        }
    }

    @Nested
    @DisplayName("ChatService")
    class ChatServiceTest {
        @Mock MensajeRepository mensajeRepository;
        @InjectMocks ChatService chatService;

        @Test
        void save_setsTimestampAndUnread() {
            Mensaje m = new Mensaje();
            when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(inv -> inv.getArgument(0));
            Mensaje saved = chatService.save(m);
            assertNotNull(saved.getTimestamp());
            assertFalse(saved.isLeido());
        }

        @Test
        void findChatHistory_delegates() {
            when(mensajeRepository.findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByTimestampAsc(
                    1L, 2L, 2L, 1L)).thenReturn(List.of(new Mensaje()));
            assertEquals(1, chatService.findChatHistory(1L, 2L).size());
        }

        @Test
        void countNewMessages_delegates() {
            when(mensajeRepository.countByRecipientIdAndSenderIdAndLeidoFalse(2L, 1L)).thenReturn(3L);
            assertEquals(3L, chatService.countNewMessages(1L, 2L));
        }
    }
}
