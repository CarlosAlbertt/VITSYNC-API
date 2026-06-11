package com.ejemplo.vitsync.config;

import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper for ownership and identity checks used to prevent IDOR
 * (Insecure Direct Object Reference) across controllers — audit finding V03.
 *
 * <p>The JWT principal is the user's NIF; resource paths use the numeric id.
 * These helpers bridge the two and centralise the "is this resource mine?"
 * decision so every controller enforces it the same way.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @return the authenticated user, resolved from the JWT NIF principal
     * @throws AccessDeniedException if there is no authentication or the user
     *         no longer exists
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("No autenticado");
        }
        return userRepository.findByNif(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));
    }

    /**
     * Resolves a user by NIF (used by the WebSocket layer, where the
     * principal comes from the STOMP session rather than the HTTP context).
     *
     * @param nif user NIF
     * @return the matching user
     * @throws AccessDeniedException if no user has that NIF
     */
    public User getCurrentUserByNif(String nif) {
        return userRepository.findByNif(nif)
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));
    }

    /** @return {@code true} if the current principal has the ADMIN role. */
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * Ensures the current user owns the resource identified by {@code ownerId}
     * (or is an admin). Use before returning or mutating user-scoped data.
     *
     * @param ownerId id of the user that owns the resource
     * @throws AccessDeniedException if the current user is neither the owner
     *         nor an admin. The message is identical to "not found" handling
     *         upstream so the API does not reveal whether the id exists.
     */
    public void requireSelfOrAdmin(Long ownerId) {
        if (isAdmin()) {
            return;
        }
        User current = getCurrentUser();
        if (!current.getId().equals(ownerId)) {
            throw new AccessDeniedException("Acceso denegado");
        }
    }
}
