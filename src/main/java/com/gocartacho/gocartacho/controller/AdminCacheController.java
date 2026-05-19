package com.gocartacho.gocartacho.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controlador de administración para la gestión de la caché en memoria.
 * Permite a los administradores forzar la limpieza de la caché cuando
 * se realizan cambios directos en la base de datos (ej. desde MongoDB Compass).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/cache")
@RequiredArgsConstructor
public class AdminCacheController {

    private final CacheManager cacheManager;

    /**
     * Limpia todas las cachés de la aplicación.
     * Útil cuando se insertan o modifican datos directamente en MongoDB
     * sin pasar por los endpoints de la API.
     *
     * <p>Solo accesible para usuarios con rol ADMIN.</p>
     */
    @PostMapping("/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> limpiarTodasLasCaches(Authentication authentication) {
        List<String> cachesLimpiadas = new ArrayList<>();

        cacheManager.getCacheNames().stream()
                .filter(java.util.Objects::nonNull)
                .forEach(cacheName -> {
                    org.springframework.cache.Cache cache = cacheManager.getCache(java.util.Objects.requireNonNull(cacheName));
                    if (cache != null) {
                        cache.clear();
                        cachesLimpiadas.add(cacheName);
                        log.info("Caché '{}' limpiada manualmente por: {}", cacheName, authentication.getName());
                    }
                });

        log.info("Limpieza total de caché ejecutada por admin: {}", authentication.getName());

        return ResponseEntity.ok(Map.of(
            "mensaje", "Caché limpiada correctamente.",
            "cachesLimpiadas", cachesLimpiadas,
            "total", cachesLimpiadas.size()
        ));
    }
}