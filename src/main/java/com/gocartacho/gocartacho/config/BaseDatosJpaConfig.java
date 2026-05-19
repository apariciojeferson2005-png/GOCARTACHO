package com.gocartacho.gocartacho.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.gocartacho.gocartacho.repository.TipoNegocioRepository;

/**
 * Configura el scan de repositorios JPA de forma aislada.
 * Solo incluye TipoNegocioRepository para evitar conflictos con MongoDB.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.gocartacho.gocartacho.repository",
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TipoNegocioRepository.class)
)
@EntityScan(basePackageClasses = com.gocartacho.gocartacho.model.TipoNegocio.class)
public class BaseDatosJpaConfig {
}
