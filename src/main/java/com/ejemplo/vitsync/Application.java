package com.ejemplo.vitsync;

import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.ejemplo.vitsync.model.User.*; // Asegúrate de importar tu Enum Role

import static com.ejemplo.vitsync.model.Role.PACIENTE;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Inicializa un usuario de prueba si no existe.
     * Recibe el UserRepository y el PasswordEncoder para crear el usuario.
     */
    @Bean
    CommandLineRunner initTestUser(UserRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {
            final String testUsername = "testuser";

            // 1. Verifica si ya existe el usuario
            if (!repository.existsByUsername(testUsername)) {

                // 2. Crea y configura un nuevo usuario (ASIGNANDO CAMPOS OBLIGATORIOS)
                User user = new User();
                user.setUsername(testUsername);
                user.setEmail("test@vitsync.com");
                user.setNombre("Usuario");
                user.setApellidos("Prueba");
                user.setPassword(passwordEncoder.encode("password123")); // Contraseña encriptada
                user.setRole(PACIENTE); // Asigna un rol por defecto
                user.setActivo(true);

                // 3. Guarda en la BBDD
                User savedUser = repository.save(user);
                System.out.println("Usuario de prueba guardado: " + savedUser.getUsername());

            } else {
                System.out.println("ℹ El usuario '" + testUsername + "' ya existe. Omite la creación.");
            }
        };
    }
}