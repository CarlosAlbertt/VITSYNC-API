package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Buscar usuario por NIF/CIF
    Optional<User> findByNif(String nif);

    // Buscar usuario por email
    Optional<User> findByEmail(String email);

    // Verificar si existe un usuario con ese NIF/CIF
    Boolean existsByNif(String nif);

    // Verificar si existe un usuario con ese email
    Boolean existsByEmail(String email);
}