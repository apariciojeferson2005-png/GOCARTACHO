package com.gocartacho.gocartacho.config;

import com.gocartacho.gocartacho.model.TipoNegocio;
import com.gocartacho.gocartacho.service.TipoNegocioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * Inicializador de datos para la tabla de Tipos de Negocio en MySQL.
 * Pre-carga los valores que antes estaban en el Enum.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(1) // Ejecutar antes que el inicializador de MongoDB si es posible
public class TipoNegocioDataInitializer implements CommandLineRunner {

    private final TipoNegocioService tipoNegocioService;

    @Override
    public void run(String... args) throws Exception {
        if (tipoNegocioService.listarTodos().isEmpty()) {
            log.info("Inicializando tabla de tipos de negocio en MySQL...");
            
            List<TipoNegocio> tiposIniciales = List.of(
                TipoNegocio.builder().nombre("Restaurante").descripcion("Establecimientos de comida y servicio a la mesa.").build(),
                TipoNegocio.builder().nombre("Bar").descripcion("Lugares especializados en bebidas y entretenimiento nocturno.").build(),
                TipoNegocio.builder().nombre("Cafeteria").descripcion("Locales para café, postres y snacks ligeros.").build(),
                TipoNegocio.builder().nombre("Hotel").descripcion("Servicios de alojamiento y hospedaje.").build(),
                TipoNegocio.builder().nombre("Tienda").descripcion("Comercios minoristas de diversos productos.").build(),
                TipoNegocio.builder().nombre("Museo").descripcion("Espacios culturales e históricos.").build(),
                TipoNegocio.builder().nombre("Otro").descripcion("Otros tipos de actividades comerciales.").build()
            );

            tiposIniciales.forEach(tipoNegocioService::guardar);
            log.info("✅ Tipos de negocio inicializados exitosamente en MySQL.");
        }
    }
}
