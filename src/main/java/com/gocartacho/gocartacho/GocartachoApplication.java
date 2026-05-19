package com.gocartacho.gocartacho;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Clase principal que inicializa la aplicación Spring Boot de Gocartacho.
 * Habilita las configuraciones automáticas y el uso de caché a nivel global.
 */
@SpringBootApplication
@EnableCaching
public class GocartachoApplication {

	public static void main(String[] args) {
		SpringApplication.run(GocartachoApplication.class, args);
	}

}