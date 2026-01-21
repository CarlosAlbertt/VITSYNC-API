package com.ejemplo.vitsync.validation;

import com.ejemplo.vitsync.model.User;

public class UserValidation {

    public void validateUser(User user) throws Exception {
        validateNif(user.getNif());
        validateRole(user);
    }

    private void validateNif(String nif) throws Exception {
        if (nif == null || !nif.matches("^[0-9]{8}[A-Za-z]$")) {
            throw new Exception("NIF inválido");
        }
    }

    private void validateRole(User user) {
        if (user.getRole() == null) {
            throw new IllegalArgumentException("El rol no puede ser nulo");
        }
    }
}
