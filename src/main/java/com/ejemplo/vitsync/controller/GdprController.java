package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.config.SecurityUtils;
import com.ejemplo.vitsync.config.ratelimit.RateLimitService;
import com.ejemplo.vitsync.service.GdprService;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

/**
 * Data-subject rights endpoints (GDPR Arts. 15, 17, 20).
 *
 * <p>Every endpoint enforces ownership via {@link SecurityUtils} (a user may
 * only act on their own data; admins may act on any). Export is rate-limited
 * to once per 24h per user to prevent abuse of an expensive operation.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@RestController
@RequestMapping("/api/users/{id}")
public class GdprController {

    private final GdprService gdprService;
    private final SecurityUtils securityUtils;
    private final RateLimitService rateLimitService;

    public GdprController(GdprService gdprService, SecurityUtils securityUtils,
                          RateLimitService rateLimitService) {
        this.gdprService = gdprService;
        this.securityUtils = securityUtils;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Right of access (Art. 15): returns all stored data as structured JSON.
     *
     * @param id user id (must be the caller's own, or admin)
     * @return the user's complete data set
     */
    @GetMapping("/my-data")
    public ResponseEntity<Map<String, Object>> myData(@PathVariable Long id) {
        securityUtils.requireSelfOrAdmin(id);
        return ResponseEntity.ok(gdprService.collectUserData(id));
    }

    /**
     * Right to portability (Art. 20): downloads a ZIP (JSON + readable text).
     * Limited to one export per 24h per user.
     *
     * @param id user id (must be the caller's own, or admin)
     * @return 200 with the ZIP; 429 with {@code Retry-After} if rate-limited
     */
    @GetMapping("/my-data/export")
    public ResponseEntity<byte[]> exportData(@PathVariable Long id) {
        securityUtils.requireSelfOrAdmin(id);

        ConsumptionProbe probe = rateLimitService.tryConsume(
                RateLimitService.Policy.GDPR_EXPORT, "export:" + id);
        if (!probe.isConsumed()) {
            long retryAfter = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
            return ResponseEntity.status(429)
                    .header("Retry-After", String.valueOf(Math.max(1, retryAfter)))
                    .build();
        }

        byte[] zip = gdprService.exportAsZip(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vitsync-datos.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    /**
     * Right to erasure (Art. 17): initiates anonymisation. Suspends the
     * account, revokes sessions, cancels future appointments and emails the
     * scheduled execution date (30-day waiting period). Actual anonymisation
     * runs after that period — see {@code docs/GDPR_PROCEDURES.md}.
     *
     * @param id user id (must be the caller's own, or admin)
     * @return 202 Accepted with the scheduled anonymisation date
     */
    @DeleteMapping("/gdpr-delete")
    public ResponseEntity<Map<String, Object>> gdprDelete(@PathVariable Long id) {
        securityUtils.requireSelfOrAdmin(id);
        LocalDate scheduled = gdprService.requestDeletion(id);
        return ResponseEntity.accepted().body(Map.of(
                "message", "Solicitud de eliminación registrada. Recibirás un email de confirmación.",
                "scheduledAnonymizationDate", scheduled.toString()));
    }
}
