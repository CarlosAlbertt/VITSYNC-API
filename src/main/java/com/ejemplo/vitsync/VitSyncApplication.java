package com.ejemplo.vitsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * VitSync API entry point.
 *
 * <p>{@code @EnableScheduling} activa la purga diaria de refresh tokens
 * caducados ({@code RefreshTokenService#purgeExpired}).</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@SpringBootApplication
@EnableScheduling
public class VitSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(VitSyncApplication.class, args);
	}

}
