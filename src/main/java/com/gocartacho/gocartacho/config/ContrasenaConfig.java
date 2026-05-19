package com.gocartacho.gocartacho.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuración para la encriptación de contraseñas de los usuarios.
 * Provee el bean PasswordEncoder utilizado por Spring Security.
 */
@Configuration
public class ContrasenaConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}