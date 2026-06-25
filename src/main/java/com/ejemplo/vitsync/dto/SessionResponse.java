package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.model.RefreshToken;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Active-session summary for the security panel. Built from a
 * {@link RefreshToken}; never exposes the token hash.
 */
@Data
@Builder
public class SessionResponse {

    private Long id;
    private String device;
    private String ipAddress;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private boolean current;

    public static SessionResponse from(RefreshToken token, boolean current) {
        LocalDateTime last = token.getLastUsedAt() != null ? token.getLastUsedAt() : token.getCreatedAt();
        return SessionResponse.builder()
                .id(token.getId())
                .device(parseDevice(token.getUserAgent()))
                .ipAddress(token.getIpAddress())
                .lastUsedAt(last)
                .createdAt(token.getCreatedAt())
                .current(current)
                .build();
    }

    /** Best-effort, dependency-free User-Agent → "Navegador · SO" label. */
    private static String parseDevice(String ua) {
        if (ua == null || ua.isBlank()) {
            return "Dispositivo desconocido";
        }
        String s = ua.toLowerCase();
        String browser = s.contains("edg") ? "Edge"
                : s.contains("chrome") || s.contains("crios") ? "Chrome"
                : s.contains("firefox") || s.contains("fxios") ? "Firefox"
                : s.contains("safari") ? "Safari"
                : "Navegador";
        String os = s.contains("windows") ? "Windows"
                : s.contains("android") ? "Android"
                : (s.contains("iphone") || s.contains("ipad") || s.contains("ios")) ? "iOS"
                : (s.contains("mac os") || s.contains("macintosh")) ? "macOS"
                : s.contains("linux") ? "Linux"
                : "—";
        return browser + " · " + os;
    }
}
