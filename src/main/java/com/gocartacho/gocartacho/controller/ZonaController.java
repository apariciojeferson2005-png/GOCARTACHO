package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.model.Zona;
import com.gocartacho.gocartacho.service.ZonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST para exponer las zonas al frontend.
 * Permite cargar las zonas dinámicamente (sin depender de Thymeleaf).
 */
@RestController
@RequestMapping("/api/v1/zonas")
@RequiredArgsConstructor
public class ZonaController {

    private final ZonaService zonaService;

    /**
     * Devuelve todas las zonas activas de la base de datos.
     * Usado por mi-negocio.html para poblar el selector de zona.
     */
    @GetMapping
    public ResponseEntity<List<Zona>> obtenerTodasLasZonas() {
        List<Zona> zonas = zonaService.obtenerTodasLasZonas();
        return ResponseEntity.ok(zonas);
    }

    /**
     * Devuelve una zona específica por ID.
     */
    @GetMapping("/{zonaId}")
    public ResponseEntity<Zona> obtenerZonaPorId(@PathVariable String zonaId) {
        return zonaService.obtenerTodasLasZonas().stream()
                .filter(z -> z.getZonaId().equals(zonaId))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Devuelve una zona por su número entero fijo (1=Centro Histórico, 2=Getsemaní, etc.).
     * Permite al frontend navegar a /explorar/1 sin conocer el ObjectId de MongoDB.
     */
    @GetMapping("/numero/{numero}")
    public ResponseEntity<Zona> obtenerZonaPorNumero(@PathVariable Integer numero) {
        Zona zona = zonaService.obtenerZonaPorNumero(numero);
        return zona != null ? ResponseEntity.ok(zona) : ResponseEntity.notFound().build();
    }
}
