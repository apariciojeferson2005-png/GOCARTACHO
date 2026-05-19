package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.dto.PuntoMapaCalorDTO;
import com.gocartacho.gocartacho.service.MapaCalorService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller para la gestión del Mapa de Calor.
 * Implementa Rate Limiting por IP/Usuario para prevenir abusos en la generación
 * de calor.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/heatmap")
@RequiredArgsConstructor
public class MapaCalorController {

    private final MapaCalorService heatmapService;

    // Caché de cubos para Rate Limiting (10 peticiones por minuto por cliente)
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Obtiene los puntos de calor en tiempo real para ser renderizados en el mapa.
     * Accesible en /api/v1/heatmap y /api/v1/heatmap/realtime (alias para el
     * frontend).
     */
    @GetMapping
    public ResponseEntity<List<PuntoMapaCalorDTO>> obtenerPuntosCalor() {
        return ResponseEntity.ok(heatmapService.obtenerPuntosCalorTiempoReal());
    }

    @GetMapping("/realtime")
    public ResponseEntity<List<PuntoMapaCalorDTO>> obtenerPuntosCalorRealtime() {
        return ResponseEntity.ok(heatmapService.obtenerPuntosCalorTiempoReal());
    }

    /**
     * Registra un nuevo punto de calor (ping de ubicación del usuario).
     * SEGURIDAD: Aplica Rate Limiting estricto.
     */
    @PostMapping("/ping")
    public ResponseEntity<String> registrarPing(@RequestBody Map<String, Double> coords, HttpServletRequest request) {
        String clientIdentifier = getClientIdentifier(request);
        Bucket bucket = buckets.computeIfAbsent(clientIdentifier, k -> createNewBucket());

        if (bucket.tryConsume(1)) {
            Double lat = coords.get("lat");
            Double lon = coords.get("lon");
            if (lat != null && lon != null) {
                com.gocartacho.gocartacho.model.PuntoCalor punto = new com.gocartacho.gocartacho.model.PuntoCalor();
                punto.setLatitud(java.math.BigDecimal.valueOf(lat));
                punto.setLongitud(java.math.BigDecimal.valueOf(lon));
                punto.setUbicacion(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(lon, lat));
                punto.setDispositivoHash(clientIdentifier);
                heatmapService.guardarPuntoCalor(punto);
                return ResponseEntity.ok("Ping registrado");
            }
            return ResponseEntity.badRequest().body("Coordenadas inválidas");
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded");
        }
    }

    /**
     * Obtiene datos históricos de afluencia para una zona.
     */
    @GetMapping("/historia/{zonaId}")
    public ResponseEntity<List<com.gocartacho.gocartacho.model.AfluenciaHistorica>> obtenerAfluenciaHistorica(@PathVariable String zonaId) {
        log.info("Consultando historial de afluencia para zona: {}", zonaId);
        return ResponseEntity.ok(heatmapService.obtenerHistorialPorZona(zonaId));
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Intenta obtener la IP real si hay un proxy (X-Forwarded-For)
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}