package com.ejemplo.vitsync.audit;

import com.ejemplo.vitsync.enums.AuditAction;
import com.ejemplo.vitsync.model.AuditLog;
import com.ejemplo.vitsync.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Writes audit records.
 *
 * <p>{@link #record} runs in a {@code REQUIRES_NEW} transaction so the audit
 * row is committed even if the surrounding business transaction later rolls
 * back — a failed action (e.g. denied access) must still leave a trace.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Persists one audit record in its own transaction.
     *
     * @param action   the action performed
     * @param targetId affected resource id, or {@code null}
     * @param success  whether the action succeeded
     * @param details  short non-sensitive detail, or {@code null}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, String targetId, boolean success, String details) {
        AuditLog log = AuditLog.builder()
                .actorNif(resolveActor())
                .action(action)
                .targetId(targetId)
                .success(success)
                .ipAddress(resolveIp())
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    /** @return the authenticated NIF, or "ANONYMOUS" if there is no principal. */
    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return "ANONYMOUS";
        }
        return auth.getName();
    }

    /** @return the client IP from the current request, or {@code null}. */
    private String resolveIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
