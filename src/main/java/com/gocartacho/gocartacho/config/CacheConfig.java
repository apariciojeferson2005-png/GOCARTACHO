package com.gocartacho.gocartacho.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Configuración de Caché multinivel con Caffeine.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Administrador de caché principal por defecto.
     * Almacena datos generales con un tiempo de vida moderado (10 minutos).
     * @return CacheManager configurado
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "comercios", "zonas", "planes", "heatmap", "estadisticas"
        );
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
        cacheManager.setCaffeine(java.util.Objects.requireNonNull(builder));
        return cacheManager;
    }

    /**
     * Administrador de caché para datos de alta volatilidad (ej. mapa de calor en vivo).
     * Tiempo de vida corto (60 segundos).
     * @return CacheManager configurado
     */
    @Bean
    public CacheManager shortLivedCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("heatmap_live");
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(100);
        cacheManager.setCaffeine(java.util.Objects.requireNonNull(builder));
        return cacheManager;
    }

    /**
     * Administrador de caché para datos estáticos o de baja modificación (ej. zonas).
     * Tiempo de vida largo (1 hora).
     * @return CacheManager configurado
     */
    @Bean
    public CacheManager longLivedCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("zonas_static");
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(50);
        cacheManager.setCaffeine(java.util.Objects.requireNonNull(builder));
        return cacheManager;
    }
}
