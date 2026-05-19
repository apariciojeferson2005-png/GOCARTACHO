package com.gocartacho.gocartacho.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades de configuración para la lógica de Mapa de Calor y Planes Inteligentes.
 * El uso de @ConfigurationProperties permite que el IDE reconozca las propiedades en application.properties.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gocartacho.heatmap")
public class HeatmapProperties {

    /**
     * Radio en kilómetros para considerar que un usuario está "cerca" de un comercio.
     */
    private double businessRadiusKm = 0.1;

    /**
     * Umbrales para determinar el nivel de afluencia.
     */
    private Threshold threshold = new Threshold();

    @Getter
    @Setter
    public static class Threshold {
        /**
         * Límite superior para el nivel 'Bajo'.
         */
        private long low = 2;

        /**
         * Límite superior para el nivel 'Medio'.
         */
        private long medium = 8;
    }
}
