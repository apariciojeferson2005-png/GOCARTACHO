package com.gocartacho.gocartacho.service;

import com.gocartacho.gocartacho.dto.ComercioDTO;
import com.gocartacho.gocartacho.model.Plan;
import java.util.List;
import java.util.Optional;
import java.util.Map;

public interface PlanService {

    List<Plan> obtenerTodasLasPlanes();
    
    Optional<Plan> obtenerPlanPorId(String planId);
    
    Map<String, Integer> calcularAfluenciaParaPlanes(List<String> planesIds);
    
    /**
     * Obtiene los comercios de una plan transformados a DTO.
     */
    List<ComercioDTO> obtenerComerciosPorPlan(String planId);
}