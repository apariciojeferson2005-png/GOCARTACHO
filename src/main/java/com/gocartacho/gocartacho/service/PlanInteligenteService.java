package com.gocartacho.gocartacho.service;

import com.gocartacho.gocartacho.dto.ComercioPlanDTO;
import com.gocartacho.gocartacho.model.NivelAfluencia;
import java.util.List;

/**
 * Interfaz que define las operaciones de negocio para PlanInteligente.
 */
public interface PlanInteligenteService {
    
    List<ComercioPlanDTO> generarPlanDinamica(double latUsuario, double lonUsuario, NivelAfluencia afluenciaDeseada, double radioKm, int maxParadas);

}
