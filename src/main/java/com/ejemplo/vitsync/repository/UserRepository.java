package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Buscar usuario por NIF/CIF
    Optional<User> findByNif(String nif);

    // Filtrar por rol EN BASE DE DATOS (antes findAll()+filtro en memoria,
    // audit finding V19), con paginación
    Page<User> findByRole(Role role, Pageable pageable);

    // Buscar usuario por email
    Optional<User> findByEmail(String email);

    // Verificar si existe un usuario con ese NIF/CIF
    Boolean existsByNif(String nif);

    // Verificar si existe un usuario con ese email
    Boolean existsByEmail(String email);

    // Buscar usuario por ID
    Optional<User> findById(Long id);

    // Actualizar solo el avatar sin validar toda la entidad
    @Modifying
    @Query("UPDATE User u SET u.avatarUrl = :avatarUrl WHERE u.id = :id")
    int updateAvatarUrl(@Param("id") Long id, @Param("avatarUrl") String avatarUrl);

}