package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.audit.AuditService;
import com.ejemplo.vitsync.dto.AuthResponse;
import com.ejemplo.vitsync.dto.LoginRequest;
import com.ejemplo.vitsync.dto.RegisterRequest;
import com.ejemplo.vitsync.enums.AuditAction;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.exception.BusinessException;
import com.ejemplo.vitsync.model.RefreshToken;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import com.ejemplo.vitsync.util.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

/**
 * Authentication use-cases: login, registration, email verification and
 * token refresh/revocation.
 *
 * <p>Security notes:</p>
 * <ul>
 *   <li>Login failures throw {@link BadCredentialsException} with a single
 *       generic message — the API must not reveal whether the NIF exists
 *       (user-enumeration hardening, audit finding V13).</li>
 *   <li>The verification code comes from {@link SecureRandom}; the previous
 *       {@code java.util.Random} was predictable (audit finding V06).</li>
 *   <li>Registration no longer returns an access token: the account is not
 *       yet verified, and issuing credentials pre-verification allowed
 *       authenticated API access from throwaway accounts (finding V07).</li>
 * </ul>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder,
            EmailService emailService, RefreshTokenService refreshTokenService, AuditService auditService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
    }

    /**
     * Authenticates a user and opens a session.
     *
     * @param request NIF + password
     * @return access token (15 min) + refresh token (7 days) + user summary
     * @throws BadCredentialsException if NIF or password are wrong (single
     *                                 generic message for both cases)
     * @throws BusinessException       if the account is not verified yet or
     *                                 has been suspended
     */
    public AuthResponse login(LoginRequest request) {
        User user;
        try {
            // Mensaje idéntico exista o no el NIF: evita enumeración de usuarios
            user = userRepository.findByNif(request.getNif())
                    .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BadCredentialsException("Credenciales inválidas");
            }

            if (!user.isVerified()) {
                throw new BusinessException("Cuenta no verificada. Por favor revisa tu correo.");
            }

            if (Boolean.TRUE.equals(user.getSuspended())) {
                throw new BusinessException("Cuenta suspendida. Contacta con soporte.");
            }
        } catch (RuntimeException ex) {
            // Trazabilidad de intentos fallidos (RGPD Art. 32 / detección de abuso)
            auditService.record(AuditAction.LOGIN_FAILURE, request.getNif(), false,
                    ex.getClass().getSimpleName());
            throw ex;
        }

        String accessToken = jwtUtil.generateToken(user.getNif(), user.getRole().name());
        String refreshToken = refreshTokenService.create(user);
        auditService.record(AuditAction.LOGIN_SUCCESS, user.getNif(), true, null);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId())
                .nif(user.getNif())
                .email(user.getEmail())
                .role(user.getRole())
                .message("Login exitoso")
                .build();
    }

    /**
     * Registers a new user and sends the email verification code.
     *
     * <p>Does NOT return tokens: the session starts only after the account
     * is verified and the user logs in.</p>
     *
     * @param request validated registration data
     * @return user summary + instruction message (no tokens)
     * @throws BusinessException if the NIF or email are already registered
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByNif(request.getNif())) {
            throw new BusinessException("No se pudo completar el registro con los datos proporcionados");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("No se pudo completar el registro con los datos proporcionados");
        }

        // La subclase concreta determina la tabla JOINED donde se persiste
        User user;
        if (request.getRole() == Role.PACIENTE) {
            user = new com.ejemplo.vitsync.model.Paciente();
        } else if (request.getRole() == Role.MEDICO) {
            user = new com.ejemplo.vitsync.model.Medico();
        } else {
            user = new User();
        }

        user.setName(request.getName());
        user.setFirstName(request.getFirstName());
        user.setSecondName(request.getSecondName());
        user.setNif(request.getNif());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setGender(request.getGender());
        user.setRole(request.getRole());
        user.setBirthDate(request.getBirthDate());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setPostCode(request.getPostCode());
        user.setCountry(request.getCountry());

        // SecureRandom: el código de 6 dígitos no debe ser predecible,
        // ya que verificar la cuenta equivale a activarla
        String randomCode = String.valueOf(secureRandom.nextInt(900000) + 100000);
        user.setVerificationCode(randomCode);
        user.setVerified(false);
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), randomCode);
        auditService.record(AuditAction.REGISTER, user.getNif(), true, null);

        return AuthResponse.builder()
                .id(user.getId())
                .nif(user.getNif())
                .email(user.getEmail())
                .role(user.getRole())
                .message("Usuario registrado. Revisa tu correo para verificar la cuenta.")
                .build();
    }

    /**
     * Verifies an account with the 6-digit code sent by email.
     *
     * @param email account email
     * @param code  6-digit verification code
     * @throws BusinessException if the email/code pair is invalid (single
     *                           generic message — no enumeration)
     */
    public void verifyAccount(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Código o email incorrectos"));

        // Comparación en tiempo constante: evita timing attacks sobre el código
        String stored = user.getVerificationCode();
        if (stored == null || code == null || !MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException("Código o email incorrectos");
        }

        user.setVerified(true);
        user.setVerificationCode(null);
        userRepository.save(user);

        emailService.sendWelcomeEmail(user.getEmail());
    }

    /**
     * Exchanges a valid refresh token for a new access token + rotated
     * refresh token.
     *
     * @param rawRefreshToken token presented by the client
     * @return fresh access token + new refresh token
     * @throws BusinessException if the refresh token is invalid/revoked/expired
     */
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken consumed = refreshTokenService.verifyAndRevoke(rawRefreshToken);
        User user = consumed.getUser();

        if (Boolean.TRUE.equals(user.getSuspended())) {
            throw new BusinessException("Cuenta suspendida. Contacta con soporte.");
        }

        String accessToken = jwtUtil.generateToken(user.getNif(), user.getRole().name());
        String newRefreshToken = refreshTokenService.create(user);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(newRefreshToken)
                .id(user.getId())
                .nif(user.getNif())
                .email(user.getEmail())
                .role(user.getRole())
                .message("Token renovado")
                .build();
    }

    /**
     * Revokes one refresh token (single-device logout). Idempotent.
     *
     * @param rawRefreshToken token presented by the client
     */
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
        auditService.record(AuditAction.LOGOUT, null, true, null);
    }

    /**
     * Revokes every active session of the authenticated user.
     *
     * @param nif NIF of the authenticated principal
     * @return number of sessions revoked
     * @throws BusinessException if the user no longer exists
     */
    public int logoutAll(String nif) {
        User user = userRepository.findByNif(nif)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        return refreshTokenService.revokeAll(user);
    }
}
