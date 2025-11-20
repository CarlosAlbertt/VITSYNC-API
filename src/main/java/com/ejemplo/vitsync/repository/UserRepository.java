package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Buscar usuario por username
    Optional<User> findByUsername(String username);

    // Buscar usuario por email
    Optional<User> findByEmail(String email);

    // Verificar si existe un username
    Boolean existsByUsername(String username);

    // Verificar si existe un email
    Boolean existsByEmail(String email);
}