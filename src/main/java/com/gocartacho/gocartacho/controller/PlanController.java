package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.dto.ComercioDTO;
import com.gocartacho.gocartacho.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/planes")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    /**
     * Devuelve las paradas (comercios) de una plan específica.
     * Llamado por planes.js al pulsar "Ver paradas".
     */
    @GetMapping("/{planId}/comercios")
    public ResponseEntity<List<ComercioDTO>> obtenerComerciosPorPlan(@PathVariable String planId) {
        List<ComercioDTO> comercios = planService.obtenerComerciosPorPlan(planId);
        return ResponseEntity.ok(comercios);
    }

    @GetMapping("/afluencia")
    public ResponseEntity<Map<String, Integer>> getAfluenciaForPlanes(@RequestParam List<String> ids) {
        Map<String, Integer> afluenciaPorPlan = planService.calcularAfluenciaParaPlanes(ids);
        return ResponseEntity.ok(afluenciaPorPlan);
    }
}