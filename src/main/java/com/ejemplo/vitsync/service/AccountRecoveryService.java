package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.audit.AuditService;
import com.ejemplo.vitsync.dto.SecurityQuestionsRequest;
import com.ejemplo.vitsync.enums.AuditAction;
import com.ejemplo.vitsync.exception.BusinessException;
import com.ejemplo.vitsync.exception.ResourceNotFoundException;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Account-recovery use-cases: storing the user's security questions and the
 * forgotten-password flow.
 *
 * <p>Security model (health data, GDPR Art. 32 — máximas garantías):</p>
 * <ul>
 *   <li>Answers are stored hashed with BCrypt and never returned. They are
 *       normalised (trim + lowercase) before hashing/matching so casing and
 *       surrounding spaces do not break a legitimate recovery.</li>
 *   <li>The reset uses <b>two factors</b>: knowledge (both answers) plus
 *       possession (a one-time code emailed to the registered address). The
 *       code is hashed in BD with a 10-minute expiry, mirroring the 2FA login
 *       flow ({@link AuthService}).</li>
 *   <li>A successful reset revokes every active session: a forgotten password
 *       may mean the account was compromised.</li>
 *   <li>Failed answer/code attempts are audited for abuse detection; the
 *       endpoints are additionally rate-limited by IP.</li>
 * </ul>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.1
 */
@Service
@Transactional
public class AccountRecoveryService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;
    private final RefreshTokenService refreshTokenService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountRecoveryService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                  EmailService emailService, AuditService auditService,
                                  RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditService = auditService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Stores (or replaces) the authenticated user's two security questions.
     * Ownership is already enforced in the controller.
     *
     * @param userId  caller's own id
     * @param request the two questions and their plaintext answers
     * @throws ResourceNotFoundException if the user does not exist
     * @throws BusinessException         if both questions are identical
     */
    public void saveQuestions(Long userId, SecurityQuestionsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + userId));

        if (request.getQ1().trim().equalsIgnoreCase(request.getQ2().trim())) {
            throw new BusinessException("Las dos preguntas de seguridad deben ser distintas");
        }

        user.setSecurityQuestion1(request.getQ1().trim());
        user.setSecurityQuestion2(request.getQ2().trim());
        user.setSecurityAnswer1(passwordEncoder.encode(normalize(request.getA1())));
        user.setSecurityAnswer2(passwordEncoder.encode(normalize(request.getA2())));
        userRepository.save(user);
    }

    /**
     * Step 1 — returns the user's two security questions so they can be
     * answered. Requires the questions to have been configured.
     *
     * @param nif user NIF
     * @return {@code {question1, question2}}
     * @throws BusinessException if the account does not exist or has no
     *                           questions configured (single generic message)
     */
    @Transactional(readOnly = true)
    public Map<String, String> getQuestionsForRecovery(String nif) {
        User user = userRepository.findByNif(nif)
                .orElseThrow(() -> new BusinessException(
                        "No hay preguntas de seguridad configuradas para esta cuenta"));

        if (user.getSecurityQuestion1() == null || user.getSecurityQuestion2() == null
                || user.getSecurityAnswer1() == null || user.getSecurityAnswer2() == null) {
            throw new BusinessException("No hay preguntas de seguridad configuradas para esta cuenta");
        }

        return Map.of(
                "question1", user.getSecurityQuestion1(),
                "question2", user.getSecurityQuestion2());
    }

    /**
     * Step 2 — verifies both answers and, if correct, emails a one-time reset
     * code (hashed in BD, 10-minute expiry).
     *
     * @param nif     user NIF
     * @param answer1 answer to the first question
     * @param answer2 answer to the second question
     * @throws BusinessException if either answer is wrong (single generic
     *                           message — no per-question oracle)
     */
    public void verifyAnswersAndSendCode(String nif, String answer1, String answer2) {
        User user = userRepository.findByNif(nif)
                .orElseThrow(() -> new BusinessException("Respuestas incorrectas"));

        if (user.getSecurityAnswer1() == null || user.getSecurityAnswer2() == null
                || !passwordEncoder.matches(normalize(answer1), user.getSecurityAnswer1())
                || !passwordEncoder.matches(normalize(answer2), user.getSecurityAnswer2())) {
            auditService.record(AuditAction.LOGIN_FAILURE, nif, false, "RECOVERY_ANSWERS");
            throw new BusinessException("Respuestas incorrectas");
        }

        String code = String.valueOf(secureRandom.nextInt(900000) + 100000);
        user.setPasswordResetCode(passwordEncoder.encode(code));
        user.setPasswordResetCodeExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        try {
            emailService.sendPasswordResetCodeEmail(user.getEmail(), code);
        } catch (Exception mailEx) {
            // best-effort: no filtramos el detalle del fallo de correo al cliente
        }
    }

    /**
     * Step 3 — validates the emailed code and sets the new password. Clears the
     * reset code, revokes every active session and emails a confirmation.
     *
     * @param nif         user NIF
     * @param code        6-digit code received by email
     * @param newPassword new password (policy already validated by the DTO)
     * @throws BusinessException if the code is wrong/expired or the new
     *                           password equals the current one
     */
    public void resetPassword(String nif, String code, String newPassword) {
        User user = userRepository.findByNif(nif)
                .orElseThrow(() -> new BusinessException("Código caducado o inválido. Reinicia el proceso."));

        if (user.getPasswordResetCode() == null || user.getPasswordResetCodeExpiry() == null
                || user.getPasswordResetCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Código caducado o inválido. Reinicia el proceso.");
        }
        if (code == null || !passwordEncoder.matches(code, user.getPasswordResetCode())) {
            auditService.record(AuditAction.LOGIN_FAILURE, nif, false, "RECOVERY_CODE");
            throw new BusinessException("Código incorrecto");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("La nueva contraseña debe ser distinta de la actual");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetCode(null);
        user.setPasswordResetCodeExpiry(null);
        userRepository.save(user);

        // Un reset de contraseña invalida cualquier sesión previa (posible robo
        // de cuenta): el usuario deberá iniciar sesión de nuevo en sus dispositivos.
        refreshTokenService.revokeAll(user);
        auditService.record(AuditAction.PASSWORD_CHANGE, nif, true, "RECOVERY");

        try {
            emailService.sendPasswordChangedEmail(user.getEmail());
        } catch (Exception mailEx) {
            // best-effort
        }
    }

    /**
     * Normalises a security answer for hashing/matching: trims surrounding
     * whitespace and lowercases, so "Madrid " and "madrid" are equivalent.
     */
    private String normalize(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase();
    }
}
