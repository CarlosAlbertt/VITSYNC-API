package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.audit.AuditService;
import com.ejemplo.vitsync.dto.AuthResponse;
import com.ejemplo.vitsync.dto.LoginRequest;
import com.ejemplo.vitsync.dto.RegisterRequest;
import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.exception.BusinessException;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}, isolating business logic with Mockito.
 *
 * <p>Reflects the v2 behaviour: login throws {@link BadCredentialsException}
 * with a single generic message (no user enumeration); unverified account
 * throws {@link BusinessException}; registration returns NO token; the
 * verification code is compared in constant time.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — authentication and registration")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuditService auditService;

    @InjectMocks private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setNif("12345678A");
        testUser.setEmail("test@vitsync.es");
        testUser.setPassword("$2a$10$hashedPasswordHere");
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
        testUser.setSuspended(false);
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
        registerRequest.setPassword("Password123!Abc");
        registerRequest.setGender(Gender.MUJER);
        registerRequest.setRole(Role.PACIENTE);
        registerRequest.setBirthDate(LocalDate.of(1995, 6, 20));
        registerRequest.setPhone("698765432");
        registerRequest.setAddress("Calle Nueva 456");
        registerRequest.setPostCode("28001");
        registerRequest.setCountry("España");
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Valid credentials return access + refresh tokens")
        void login_withValidCredentials_returnsAuthResponse() {
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password123", testUser.getPassword())).thenReturn(true);
            when(jwtUtil.generateToken("12345678A", "PACIENTE")).thenReturn("jwt.token.here");
            when(refreshTokenService.create(testUser, null, null)).thenReturn("refresh-token");

            AuthResponse response = authService.login(loginRequest);

            assertNotNull(response);
            assertEquals("jwt.token.here", response.getToken());
            assertEquals("refresh-token", response.getRefreshToken());
            assertEquals("12345678A", response.getNif());
            assertEquals(Role.PACIENTE, response.getRole());

            verify(jwtUtil).generateToken("12345678A", "PACIENTE");
            verify(refreshTokenService).create(testUser, null, null);
        }

        @Test
        @DisplayName("Unknown NIF throws BadCredentialsException (generic message)")
        void login_withUnknownNif_throwsBadCredentials() {
            when(userRepository.findByNif("99999999R")).thenReturn(Optional.empty());
            loginRequest.setNif("99999999R");

            BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                    () -> authService.login(loginRequest));

            assertEquals("Credenciales inválidas", ex.getMessage());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Wrong password throws BadCredentialsException (generic message)")
        void login_withWrongPassword_throwsBadCredentials() {
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("WrongPass123", testUser.getPassword())).thenReturn(false);
            loginRequest.setPassword("WrongPass123");

            BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                    () -> authService.login(loginRequest));

            assertEquals("Credenciales inválidas", ex.getMessage());
            verify(jwtUtil, never()).generateToken(anyString(), anyString());
        }

        @Test
        @DisplayName("Unverified account throws BusinessException")
        void login_withUnverifiedAccount_throwsBusiness() {
            testUser.setVerified(false);
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password123", testUser.getPassword())).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(loginRequest));

            assertTrue(ex.getMessage().contains("no verificada"));
            verify(jwtUtil, never()).generateToken(anyString(), anyString());
        }

        @Test
        @DisplayName("Failed login is audited as LOGIN_FAILURE")
        void login_failure_isAudited() {
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.empty());
            assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
            verify(auditService).record(eq(com.ejemplo.vitsync.enums.AuditAction.LOGIN_FAILURE),
                    anyString(), eq(false), anyString());
        }
    }

    @Nested
    @DisplayName("Registration")
    class RegisterTests {

        @Test
        @DisplayName("Valid data saves a hashed user, sends email, returns NO token")
        void register_withValidData_createsUserAndSendsEmail() {
            when(userRepository.existsByNif("87654321B")).thenReturn(false);
            when(userRepository.existsByEmail("nuevo@vitsync.es")).thenReturn(false);
            when(passwordEncoder.encode("Password123!Abc")).thenReturn("$2a$10$encodedHash");

            AuthResponse response = authService.register(registerRequest);

            assertNotNull(response);
            assertNull(response.getToken(), "El registro no debe emitir access token");
            assertNull(response.getRefreshToken());

            // El password se guarda hasheado, nunca en claro
            verify(passwordEncoder).encode("Password123!Abc");
            verify(userRepository).save(argThat(u -> "$2a$10$encodedHash".equals(u.getPassword())));
            verify(emailService).sendVerificationEmail(eq("nuevo@vitsync.es"), anyString());
        }

        @Test
        @DisplayName("Duplicate NIF throws BusinessException")
        void register_withDuplicateNif_throws() {
            when(userRepository.existsByNif("87654321B")).thenReturn(true);

            assertThrows(BusinessException.class, () -> authService.register(registerRequest));
            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("Duplicate email throws BusinessException")
        void register_withDuplicateEmail_throws() {
            when(userRepository.existsByNif("87654321B")).thenReturn(false);
            when(userRepository.existsByEmail("nuevo@vitsync.es")).thenReturn(true);

            assertThrows(BusinessException.class, () -> authService.register(registerRequest));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Account verification")
    class VerifyAccountTests {

        @Test
        @DisplayName("Correct code verifies the user")
        void verifyAccount_withCorrectCode_verifiesUser() {
            testUser.setVerified(false);
            testUser.setVerificationCode("123456");
            when(userRepository.findByEmail("test@vitsync.es")).thenReturn(Optional.of(testUser));

            assertDoesNotThrow(() -> authService.verifyAccount("test@vitsync.es", "123456"));

            assertTrue(testUser.isVerified());
            assertNull(testUser.getVerificationCode());
            verify(userRepository).save(testUser);
            verify(emailService).sendWelcomeEmail("test@vitsync.es");
        }

        @Test
        @DisplayName("Wrong code throws BusinessException (generic message)")
        void verifyAccount_withWrongCode_throws() {
            testUser.setVerificationCode("123456");
            when(userRepository.findByEmail("test@vitsync.es")).thenReturn(Optional.of(testUser));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.verifyAccount("test@vitsync.es", "999999"));

            assertEquals("Código o email incorrectos", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Unknown email throws BusinessException")
        void verifyAccount_withUnknownEmail_throws() {
            when(userRepository.findByEmail("fake@vitsync.es")).thenReturn(Optional.empty());

            assertThrows(BusinessException.class,
                    () -> authService.verifyAccount("fake@vitsync.es", "123456"));
        }
    }
}
