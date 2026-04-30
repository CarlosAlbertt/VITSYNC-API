package com.ejemplo.vitsync.util;

import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SecurityUtil {

    private final UserRepository userRepository;

    public SecurityUtil(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getCurrentNif() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public User getCurrentUser() {
        return userRepository.findByNif(getCurrentNif())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }

    /** Lanza 403 si el usuario autenticado no es el dueño del recurso ni ADMIN. */
    public void assertOwnerOrAdmin(Long resourceOwnerId) {
        User current = getCurrentUser();
        if (current.getRole() != Role.ADMIN && !current.getId().equals(resourceOwnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso no autorizado a este recurso");
        }
    }
}
