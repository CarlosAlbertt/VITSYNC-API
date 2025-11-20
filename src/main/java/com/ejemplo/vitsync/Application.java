package com.ejemplo.vitsync;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.context.support.BeanDefinitionDsl;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner test(UserRepository repository) {
        return args -> {
            // Verifica si ya existe el usuario
            if (!repository.existsByUsername("testuser")) {
                User user = new User();
                repository.save(user);
                System.out.println("Usuario guardado: " + user);
            } else {
                System.out.println("ℹEl usuario 'testuser' ya existe");
            }
        };
    }
}