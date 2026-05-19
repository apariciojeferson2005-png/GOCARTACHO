package com.gocartacho.gocartacho.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Configura el scan de repositorios MongoDB de forma segura.
 * Se añade un filtro para evitar que intente instanciar repositorios JPA (MySQL)
 * como si fueran de MongoDB, lo cual es una causa común de fallo al inicializar.
 */
@Configuration
@EnableMongoRepositories(
    basePackages = "com.gocartacho.gocartacho.repository",
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MongoRepository.class)
)
public class BaseDatosMongoConfig {
}