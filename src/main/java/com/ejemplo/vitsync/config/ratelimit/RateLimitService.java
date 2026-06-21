package com.ejemplo.vitsync.config.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter backed by Bucket4j (token-bucket algorithm).
 *
 * <p>Buckets are keyed by {@code policy + ":" + discriminator} (the
 * discriminator is usually the client IP or an account id) and held in a
 * {@link ConcurrentHashMap}. This is correct for a single instance, which is
 * the current Render topology. If the service is scaled horizontally, swap
 * the map for {@code bucket4j-redis} so limits are shared across nodes
 * (documented in {@code docs/SECURITY.md}).</p>
 *
 * <p>Each {@link Policy} encodes the limits required by the audit (V06/V11):
 * login 5/15min, register 3/h, verify 10/account, GDPR export 1/24h.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Service
public class RateLimitService {

    /**
     * Named rate-limit policies. The capacity is also the burst size; the
     * refill is greedy over the window.
     */
    public enum Policy {
        /** Login: 5 intentos por ventana de 15 minutos. */
        LOGIN(5, Duration.ofMinutes(15)),
        /** Registro: 3 por hora (anti alta masiva de cuentas). */
        REGISTER(3, Duration.ofHours(1)),
        /** Verificación: 10 intentos de código por cuenta. */
        VERIFY(10, Duration.ofHours(1)),
        /** Exportación RGPD: 1 cada 24 horas por usuario. */
        GDPR_EXPORT(1, Duration.ofHours(24)),
        /** Recuperación de contraseña: 5 intentos por hora (anti fuerza bruta de respuestas/código). */
        PASSWORD_RECOVERY(5, Duration.ofHours(1));

        private final long capacity;
        private final Duration window;

        Policy(long capacity, Duration window) {
            this.capacity = capacity;
            this.window = window;
        }
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Tries to consume one token for the given policy and key.
     *
     * @param policy        the limit to apply
     * @param discriminator caller identity (IP, account id, …)
     * @return a probe: {@code isConsumed()} tells if the request is allowed,
     *         {@code getNanosToWaitForRefill()} feeds the {@code Retry-After}
     *         header on rejection
     */
    public ConsumptionProbe tryConsume(Policy policy, String discriminator) {
        Bucket bucket = buckets.computeIfAbsent(policy.name() + ":" + discriminator,
                k -> newBucket(policy));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    /** Builds a bucket whose single bandwidth matches the policy window. */
    private Bucket newBucket(Policy policy) {
        Bandwidth limit = Bandwidth.classic(policy.capacity,
                Refill.greedy(policy.capacity, policy.window));
        return Bucket.builder().addLimit(limit).build();
    }
}
