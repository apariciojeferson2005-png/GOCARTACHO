package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.EstadoComercio;
import com.gocartacho.gocartacho.service.ComercioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
/**
 * Controlador para manejar las peticiones HTTP relacionadas con Estadisticas.
 */
public class EstadisticasController {

    // CORRECCIÓN: Inyectamos ComercioService en lugar del repositorio directamente.
    // Los controladores no deben acceder a la capa de datos sin pasar por el
    // servicio.
    private final ComercioService comercioService;
    private final com.gocartacho.gocartacho.service.ZonaService zonaService;
    private final com.gocartacho.gocartacho.service.TipoNegocioService tipoNegocioService;

    @GetMapping("/distribucion-zonas")
    public ResponseEntity<Map<String, Long>> getDistribucionPorZona() {
        List<Comercio> comercios = comercioService.obtenerTodosSinFiltro();
        Map<String, String> zonasMap = zonaService.obtenerTodasLasZonas().stream()
                .collect(Collectors.toMap(com.gocartacho.gocartacho.model.Zona::getZonaId,
                        com.gocartacho.gocartacho.model.Zona::getNombre));

        Map<String, Long> stats = comercios.stream()
                .collect(Collectors.groupingBy(c -> {
                    if (c.getZonaId() != null)
                        return zonasMap.getOrDefault(c.getZonaId(), "Zona Desconocida");
                    return "Sin Zona";
                }, Collectors.counting()));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/distribucion-categorias")
    public ResponseEntity<Map<String, Long>> getDistribucionPorCategoria() {
        List<Comercio> comercios = comercioService.obtenerTodosSinFiltro();
        Map<Long, String> tiposMap = tipoNegocioService.listarTodos().stream()
                .collect(Collectors.toMap(com.gocartacho.gocartacho.model.TipoNegocio::getId,
                        com.gocartacho.gocartacho.model.TipoNegocio::getNombre));

        Map<String, Long> stats = comercios.stream()
                .collect(Collectors.groupingBy(c -> {
                    if (c.getTipoNegocioId() != null)
                        return tiposMap.getOrDefault(c.getTipoNegocioId(), "Otros");
                    return "Otros";
                }, Collectors.counting()));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/distribucion-estados")
    public ResponseEntity<Map<String, Long>> getDistribucionPorEstado() {
        List<Comercio> comercios = comercioService.obtenerTodosSinFiltro();
        Map<String, Long> stats = comercios.stream()
                .collect(Collectors.groupingBy(c -> {
                    EstadoComercio estado = c.getEstadoAprobacion();
                    return estado != null ? estado.name() : "PENDIENTE";
                }, Collectors.counting()));
        return ResponseEntity.ok(stats);
    }
}
