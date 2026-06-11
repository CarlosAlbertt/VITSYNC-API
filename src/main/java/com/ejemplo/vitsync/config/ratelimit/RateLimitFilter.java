package com.ejemplo.vitsync.config.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Servlet filter that throttles abuse-prone authentication endpoints before
 * they reach the controllers.
 *
 * <p>Runs early (ordered ahead of the JWT filter) so brute-force attempts are
 * rejected cheaply. Limits are keyed by client IP. On rejection it returns
 * <b>429 Too Many Requests</b> with a {@code Retry-After} header (seconds),
 * as required by the audit (V06/V11).</p>
 *
 * <p>Mapped policies:</p>
 * <ul>
 *   <li>{@code POST /api/auth/login} → 5 / 15 min per IP</li>
 *   <li>{@code POST /api/auth/register} → 3 / hour per IP</li>
 *   <li>{@code POST /api/auth/verify} → 10 / hour per IP</li>
 * </ul>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        RateLimitService.Policy policy = resolvePolicy(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        ConsumptionProbe probe = rateLimitService.tryConsume(policy, clientIp);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
        response.setStatus(429); // 429 Too Many Requests (no constante en la API servlet)
        response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfterSeconds)));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\","
                        + "\"message\":\"Demasiados intentos. Inténtalo de nuevo más tarde.\"}");
    }

    /** Maps POST auth paths to a policy; everything else is unthrottled here. */
    private RateLimitService.Policy resolvePolicy(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        if (path.endsWith("/api/auth/login")) {
            return RateLimitService.Policy.LOGIN;
        }
        if (path.endsWith("/api/auth/register")) {
            return RateLimitService.Policy.REGISTER;
        }
        if (path.endsWith("/api/auth/verify")) {
            return RateLimitService.Policy.VERIFY;
        }
        return null;
    }

    /**
     * Resolves the client IP, honouring {@code X-Forwarded-For} since the app
     * runs behind the Render proxy (the first hop is the real client).
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
