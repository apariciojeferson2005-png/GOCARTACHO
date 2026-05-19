package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.dto.ComercioPlanDTO;
import com.gocartacho.gocartacho.model.NivelAfluencia;
import com.gocartacho.gocartacho.service.PlanInteligenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para manejar las peticiones HTTP relacionadas con PlanInteligente.
 */
@RestController
@RequestMapping("/api/v1/planes-inteligentes")
public class PlanInteligenteController {

    private final PlanInteligenteService planInteligenteService;

    public PlanInteligenteController(PlanInteligenteService planInteligenteService) {
        this.planInteligenteService = planInteligenteService;
    }

    @GetMapping("/generar")
    public ResponseEntity<List<ComercioPlanDTO>> generarPlan(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "Bajo") NivelAfluencia afluencia,
            @RequestParam(defaultValue = "2.0") double radio) { // Por defecto 2KM a la redonda
        return ResponseEntity.ok(planInteligenteService.generarPlanDinamica(lat, lng, afluencia, radio, 5));
    }
}
