package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.Plan;
import com.gocartacho.gocartacho.repository.ComercioRepository;
import com.gocartacho.gocartacho.repository.PlanComercioRepository;
import com.gocartacho.gocartacho.repository.PlanRepository;
import com.gocartacho.gocartacho.service.MapaCalorService;
import com.gocartacho.gocartacho.service.TipoNegocioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
public class PlanServiceImplTest {

    @Mock
    private PlanRepository planRepository;
    @Mock
    private PlanComercioRepository planComercioRepository;
    @Mock
    private ComercioRepository comercioRepository;
    @Mock
    private MapaCalorService mapaCalorService;
    @Mock
    private TipoNegocioService tipoNegocioService;

    @InjectMocks
    private PlanServiceImpl planService;

    @Test
    void obtenerPlanPorId_Exito() {
        Plan plan = new Plan();
        plan.setPlanId("plan-1");
        plan.setNombrePlan("Plan de Prueba");

        when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        Optional<Plan> resultado = planService.obtenerPlanPorId("plan-1");

        assertTrue(resultado.isPresent());
        assertEquals("Plan de Prueba", resultado.get().getNombrePlan());
        verify(planRepository, times(1)).findById("plan-1");
    }

    @Test
    void obtenerPlanPorId_NoEncontrado() {
        when(planRepository.findById("invalid")).thenReturn(Optional.empty());
        Optional<Plan> resultado = planService.obtenerPlanPorId("invalid");
        assertTrue(resultado.isEmpty());
    }
}
