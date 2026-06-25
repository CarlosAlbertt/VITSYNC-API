package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.audit.AuditService;
import com.ejemplo.vitsync.dto.SecurityQuestionsRequest;
import com.ejemplo.vitsync.enums.AuditAction;
import com.ejemplo.vitsync.exception.BusinessException;
import com.ejemplo.vitsync.exception.ResourceNotFoundException;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AccountRecoveryService}: storing security questions and
 * the forgotten-password flow (questions + emailed one-time code).
 *
 * <p>Key invariants verified: answers are hashed (never stored/compared in
 * clear), wrong answers/codes are rejected with generic messages and audited,
 * a successful reset revokes every session and emails a confirmation.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountRecoveryService — security questions and password recovery")
class AccountRecoveryServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AccountRecoveryService recoveryService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setNif("12345678A");
        user.setEmail("test@vitsync.es");
        user.setPassword("$2a$10$currentHash");
    }

    @Nested
    @DisplayName("Save questions")
    class SaveQuestions {

        private SecurityQuestionsRequest request() {
            SecurityQuestionsRequest req = new SecurityQuestionsRequest();
            req.setQ1("¿Primera mascota?");
            req.setA1("  Rex ");
            req.setQ2("¿Ciudad natal?");
            req.setA2("Valencia");
            return req;
        }

        @Test
        @DisplayName("hashes both answers (normalised) and stores the questions")
        void saveQuestions_hashesAnswers() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(anyString())).thenReturn("HASH");

            recoveryService.saveQuestions(1L, request());

            // Respuesta normalizada (trim + lowercase) antes de hashear
            verify(passwordEncoder).encode("rex");
            verify(passwordEncoder).encode("valencia");
            assertEquals("¿Primera mascota?", user.getSecurityQuestion1());
            assertEquals("HASH", user.getSecurityAnswer1());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("rejects two identical questions")
        void saveQuestions_identicalQuestions_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            SecurityQuestionsRequest req = request();
            req.setQ2(" ¿primera MASCOTA? ");

            assertThrows(BusinessException.class, () -> recoveryService.saveQuestions(1L, req));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("404 when the user does not exist")
        void saveQuestions_unknownUser_throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> recoveryService.saveQuestions(99L, request()));
        }
    }

    @Nested
    @DisplayName("Step 1 — get questions")
    class GetQuestions {

        @Test
        @DisplayName("returns both questions when configured")
        void getQuestions_returnsThem() {
            user.setSecurityQuestion1("Q1");
            user.setSecurityQuestion2("Q2");
            user.setSecurityAnswer1("h1");
            user.setSecurityAnswer2("h2");
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(user));

            Map<String, String> result = recoveryService.getQuestionsForRecovery("12345678A");

            assertEquals("Q1", result.get("question1"));
            assertEquals("Q2", result.get("question2"));
        }

        @Test
        @DisplayName("generic error when no questions configured")
        void getQuestions_notConfigured_throws() {
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(user));
            assertThrows(BusinessException.class,
                    () -> recoveryService.getQuestionsForRecovery("12345678A"));
        }

        @Test
        @DisplayName("generic error when the account does not exist (no enumeration)")
        void getQuestions_unknownNif_throws() {
            when(userRepository.findByNif("00000000T")).thenReturn(Optional.empty());
            assertThrows(BusinessException.class,
                    () -> recoveryService.getQuestionsForRecovery("00000000T"));
        }
    }

    @Nested
    @DisplayName("Step 2 — verify answers")
    class VerifyAnswers {

        @BeforeEach
        void configureQuestions() {
            user.setSecurityAnswer1("h1");
            user.setSecurityAnswer2("h2");
        }

        @Test
        @DisplayName("correct answers email a one-time code stored hashed with expiry")
        void verify_correctAnswers_sendsCode() {
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("rex", "h1")).thenReturn(true);
            when(passwordEncoder.matches("valencia", "h2")).thenReturn(true);
            when(passwordEncoder.encode(anyString())).thenReturn("CODEHASH");

            recoveryService.verifyAnswersAndSendCode("12345678A", " Rex ", "VALENCIA");

            assertEquals("CODEHASH", user.getPasswordResetCode());
            assertNotNull(user.getPasswordResetCodeExpiry());
            assertTrue(user.getPasswordResetCodeExpiry().isAfter(LocalDateTime.now()));
            verify(emailService).sendPasswordResetCodeEmail(eq("test@vitsync.es"), anyString());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("wrong answer is rejected, audited and sends no code")
        void verify_wrongAnswer_throwsAndAudits() {
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("rex", "h1")).thenReturn(true);
            when(passwordEncoder.matches(anyString(), eq("h2"))).thenReturn(false);

            assertThrows(BusinessException.class,
                    () -> recoveryService.verifyAnswersAndSendCode("12345678A", "Rex", "wrong"));

            verify(auditService).record(eq(AuditAction.LOGIN_FAILURE), eq("12345678A"), eq(false), anyString());
            verify(emailService, never()).sendPasswordResetCodeEmail(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Step 3 — reset password")
    class ResetPassword {

        @BeforeEach
        void configureCode() {
            user.setPasswordResetCode("codeHash");
            user.setPasswordResetCodeExpiry(LocalDateTime.now().plusMinutes(5));
        }

        @Test
        @DisplayName("valid code sets the new password, clears the code, revokes sessions and emails")
        void reset_validCode_succeeds() {
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("123456", "codeHash")).thenReturn(true);
            when(passwordEncoder.matches("NewPass123!Abc", "$2a$10$currentHash")).thenReturn(false);
            when(passwordEncoder.encode("NewPass123!Abc")).thenReturn("newHash");

            recoveryService.resetPassword("12345678A", "123456", "NewPass123!Abc");

            assertEquals("newHash", user.getPassword());
            assertNull(user.getPasswordResetCode());
            assertNull(user.getPasswordResetCodeExpiry());
            verify(refreshTokenService).revokeAll(user);
            verify(emailService).sendPasswordChangedEmail("test@vitsync.es");
            verify(auditService).record(eq(AuditAction.PASSWORD_CHANGE), eq("12345678A"), eq(true), anyString());
        }

        @Test
        @DisplayName("expired code is rejected")
        void reset_expiredCode_throws() {
            user.setPasswordResetCodeExpiry(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(user));

            assertThrows(BusinessException.class,
                    () -> recoveryService.resetPassword("12345678A", "123456", "NewPass123!Abc"));
            verify(refreshTokenService, never()).revokeAll(any());
        }

        @Test
        @DisplayName("wrong code is rejected and audited")
        void reset_wrongCode_throwsAndAudits() {
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("000000", "codeHash")).thenReturn(false);

            assertThrows(BusinessException.class,
                    () -> recoveryService.resetPassword("12345678A", "000000", "NewPass123!Abc"));
            verify(auditService).record(eq(AuditAction.LOGIN_FAILURE), eq("12345678A"), eq(false), anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a new password equal to the current one")
        void reset_samePassword_throws() {
            when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("123456", "codeHash")).thenReturn(true);
            when(passwordEncoder.matches("NewPass123!Abc", "$2a$10$currentHash")).thenReturn(true);

            assertThrows(BusinessException.class,
                    () -> recoveryService.resetPassword("12345678A", "123456", "NewPass123!Abc"));
            verify(refreshTokenService, never()).revokeAll(any());
        }
    }
}
