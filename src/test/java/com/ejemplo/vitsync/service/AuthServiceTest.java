package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.AuthResponse;
import com.ejemplo.vitsync.dto.LoginRequest;
import com.ejemplo.vitsync.dto.RegisterRequest;
import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import com.ejemplo.vitsync.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AuthService.
 *
 * Usa Mockito para aislar la lógica de negocio de las dependencias
 * (UserRepository, JwtUtil, PasswordEncoder, EmailService).
 *
 * Estructura:
 * - Login: credenciales correctas, NIF inexistente, contraseña incorrecta, cuenta no verificada
 * - Registro: registro exitoso, NIF duplicado, email duplicado
 * - Verificación: código correcto, código incorrecto, email inexistente
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Autenticación y Registro")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    // ─── Datos de prueba reutilizables ──────────────────────────────────

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setNif("12345678A");
        testUser.setEmail("test@vitsync.es");
        testUser.setPassword("$2a$10$hashedPasswordHere"); // BCrypt hash simulado
        testUser.setName("Test");
        testUser.setFirstName("Usuario");
        testUser.setSecondName("Prueba");
        testUser.setRole(Role.PACIENTE);
        testUser.setGender(Gender.HOMBRE);
        testUser.setBirthDate(LocalDate.of(1990, 1, 15));
        testUser.setPhone("612345678");
        testUser.setAddress("Calle Test 123");
        testUser.setPostCode("46001");
        testUser.setCountry("España");
        testUser.setVerified(true);
        testUser.setVerificationCode(null);

        loginRequest = new LoginRequest();
        loginRequest.setNif("12345678A");
        loginRequest.setPassword("Password123");

        registerRequest = new RegisterRequest();
        registerRequest.setName("Nuevo");
        registerRequest.setFirstName("Usuario");
        registerRequest.setSecondName("Test");
        registerRequest.setNif("87654321B");
        registerRequest.setEmail("nuevo@vitsync.es");
        registerRequest.setPassword("Password123");
        registerRequest.setGender(Gender.MUJER);
        registerRequest.setRole(Role.PACIENTE);
        registerRequest.setBirthDate(LocalDate.of(1995, 6, 20));
        registerRequest.setPhone("698765432");
        registerRequest.setAddress("Calle Nueva 456");
        registerRequest.setPostCode("28001");
        registerRequest.setCountry("España");
    }

    // ─── LOGIN ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Login exitoso con credenciales válidas")
        void login_withValidCredentials_returnsAuthResponse() {
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password123", testUser.getPassword())).thenReturn(true);
            when(jwtUtil.generateToken("12345678A", "PACIENTE")).thenReturn("jwt.token.here");

            AuthResponse response = authService.login(loginRequest);

            assertNotNull(response);
            assertEquals("jwt.token.here", response.getToken());
            assertEquals("12345678A", response.getNif());
            assertEquals(Role.PACIENTE, response.getRole());
            assertEquals("Login exitoso", response.getMessage());

            verify(userRepository).findByNif("12345678A");
            verify(passwordEncoder).matches("Password123", testUser.getPassword());
            verify(jwtUtil).generateToken("12345678A", "PACIENTE");
        }

        @Test
        @DisplayName("Login falla con NIF inexistente")
        void login_withUnknownNif_throwsException() {
            when(userRepository.findByNif("99999999Z")).thenReturn(Optional.empty());
            loginRequest.setNif("99999999Z");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.login(loginRequest));

            assertEquals("Usuario no encontrado", ex.getMessage());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Login falla con contraseña incorrecta")
        void login_withWrongPassword_throwsException() {
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("WrongPass123", testUser.getPassword())).thenReturn(false);
            loginRequest.setPassword("WrongPass123");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.login(loginRequest));

            assertEquals("Contraseña incorrecta", ex.getMessage());
            verify(jwtUtil, never()).generateToken(anyString(), anyString());
        }

        @Test
        @DisplayName("Login falla con cuenta no verificada")
        void login_withUnverifiedAccount_throwsException() {
            testUser.setVerified(false);
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password123", testUser.getPassword())).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.login(loginRequest));

            assertTrue(ex.getMessage().contains("no verificada"));
            verify(jwtUtil, never()).generateToken(anyString(), anyString());
        }
    }

    // ─── REGISTRO ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Registro")
    class RegisterTests {

        @Test
        @DisplayName("Registro exitoso de nuevo paciente")
        void register_withValidData_createsUserAndSendsEmail() {
            when(userRepository.existsByNif("87654321B")).thenReturn(false);
            when(userRepository.existsByEmail("nuevo@vitsync.es")).thenReturn(false);
            when(passwordEncoder.encode("Password123")).thenReturn("$2a$10$encodedHash");
            when(jwtUtil.generateToken(eq("87654321B"), eq("PACIENTE"))).thenReturn("new.jwt.token");

            AuthResponse response = authService.register(registerRequest);

            assertNotNull(response);
            assertEquals("new.jwt.token", response.getToken());
            assertEquals("Usuario registrado exitosamente", response.getMessage());

            // Verificar que se guardó el usuario y se envió email
            verify(userRepository).save(any(User.class));
            verify(emailService).sendVerificationEmail(eq("nuevo@vitsync.es"), anyString());
        }

        @Test
        @DisplayName("Registro falla con NIF duplicado")
        void register_withDuplicateNif_throwsException() {
            when(userRepository.existsByNif("87654321B")).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(registerRequest));

            assertTrue(ex.getMessage().contains("ya está en uso"));
            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("Registro falla con email duplicado")
        void register_withDuplicateEmail_throwsException() {
            when(userRepository.existsByNif("87654321B")).thenReturn(false);
            when(userRepository.existsByEmail("nuevo@vitsync.es")).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(registerRequest));

            assertTrue(ex.getMessage().contains("email ya está registrado"));
            verify(userRepository, never()).save(any());
        }
    }

    // ─── VERIFICACIÓN ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Verificación de cuenta")
    class VerifyAccountTests {

        @Test
        @DisplayName("Verificación exitosa con código correcto")
        void verifyAccount_withCorrectCode_verifiesUser() {
            testUser.setVerified(false);
            testUser.setVerificationCode("123456");
            when(userRepository.findByEmail("test@vitsync.es")).thenReturn(Optional.of(testUser));

            assertDoesNotThrow(() -> authService.verifyAccount("test@vitsync.es", "123456"));

            assertTrue(testUser.isVerified(), "El usuario debe estar verificado");
            assertNull(testUser.getVerificationCode(), "El código debe ser null tras verificar");
            verify(userRepository).save(testUser);
            verify(emailService).sendWelcomeEmail("test@vitsync.es");
        }

        @Test
        @DisplayName("Verificación falla con código incorrecto")
        void verifyAccount_withWrongCode_throwsException() {
            testUser.setVerificationCode("123456");
            when(userRepository.findByEmail("test@vitsync.es")).thenReturn(Optional.of(testUser));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.verifyAccount("test@vitsync.es", "999999"));

            assertEquals("Código de verificación incorrecto", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Verificación falla con email inexistente")
        void verifyAccount_withUnknownEmail_throwsException() {
            when(userRepository.findByEmail("fake@vitsync.es")).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.verifyAccount("fake@vitsync.es", "123456"));

            assertEquals("El email no está registrado", ex.getMessage());
        }
    }
}
