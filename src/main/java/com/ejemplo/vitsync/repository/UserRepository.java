package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> { //REPOSITORIO PARA EL MANEJO DE LA ENTIDAD USUARIO

}