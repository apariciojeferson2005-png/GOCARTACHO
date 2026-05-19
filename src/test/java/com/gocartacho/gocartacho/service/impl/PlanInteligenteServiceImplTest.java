package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.dto.ComercioPlanDTO;
import com.gocartacho.gocartacho.dto.PuntoMapaCalorDTO;
import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.EstadoComercio;
import com.gocartacho.gocartacho.model.NivelAfluencia;
import com.gocartacho.gocartacho.repository.ComercioRepository;
import com.gocartacho.gocartacho.service.MapaCalorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
public class PlanInteligenteServiceImplTest {

    @Mock
    private ComercioRepository comercioRepository;

    @Mock
    private MapaCalorService heatmapService;

    @InjectMocks
    private PlanInteligenteServiceImpl planInteligenteService;

    private Comercio comercio1;
    private Comercio comercio2;
    private Comercio comercio3;

    @BeforeEach
    void setUp() {
        comercio1 = Comercio.builder()
                .comercioId("1")
                .nombre("Restaurante Centro")
                .latitud(new BigDecimal("10.42"))
                .longitud(new BigDecimal("-75.54"))
                .estadoAprobacion(EstadoComercio.APROBADO)
                .build();

        comercio2 = Comercio.builder()
                .comercioId("2")
                .nombre("Café Cerca")
                .latitud(new BigDecimal("10.43"))
                .longitud(new BigDecimal("-75.54"))
                .estadoAprobacion(EstadoComercio.APROBADO)
                .build();

        // Lejos del usuario
        comercio3 = Comercio.builder()
                .comercioId("3")
                .nombre("Comercio Lejano")
                .latitud(new BigDecimal("10.90"))
                .longitud(new BigDecimal("-75.10"))
                .estadoAprobacion(EstadoComercio.APROBADO)
                .build();

        // Inicializar campos @Value
        ReflectionTestUtils.setField(java.util.Objects.requireNonNull(planInteligenteService), "businessRadiusKm", 0.1);
        ReflectionTestUtils.setField(java.util.Objects.requireNonNull(planInteligenteService), "thresholdLow", 2L);
        ReflectionTestUtils.setField(java.util.Objects.requireNonNull(planInteligenteService), "thresholdMedium", 8L);
    }

    @Test
    void generarPlan_ConCandidatosMultiples_GeneraPlanBase() {
        // Mock de DB y calor (Nivel Bajo - 1 punto cerca del centro)
        when(comercioRepository.findByEstadoAprobacionAndUbicacionNear(eq(EstadoComercio.APROBADO), any(), any()))
                .thenReturn(Arrays.asList(comercio1, comercio2));
        when(heatmapService.obtenerPuntosCalorTiempoReal()).thenReturn(
                List.of(new PuntoMapaCalorDTO(10.42, -75.54, 1.0))
        );

        List<ComercioPlanDTO> plan = planInteligenteService.generarPlanDinamica(
                10.41, -75.54, NivelAfluencia.Bajo, 10.0, 5);

        assertNotNull(plan);
        assertEquals(2, plan.size(), "Debe devolver ambos comercios válidos");
        
        // Verifica que se hace por distancia, comercio 1 (10.42) es más cerca de (10.41) que el 2 (10.43).
        assertEquals("1", plan.get(0).getComercioId()); 
        assertEquals("2", plan.get(1).getComercioId());
    }

    @Test
    void generarPlan_IgnoraComerciosFueraDelRadio() {
        when(comercioRepository.findByEstadoAprobacionAndUbicacionNear(eq(EstadoComercio.APROBADO), any(), any()))
                .thenReturn(Arrays.asList(comercio1, comercio3));
        when(heatmapService.obtenerPuntosCalorTiempoReal()).thenReturn(Collections.emptyList()); // Sin calor = BAJA

        // Radio = 5km. Comercio 3 está en lat 10.9 (~50km). Debe quedar fuera.
        List<ComercioPlanDTO> plan = planInteligenteService.generarPlanDinamica(
                10.41, -75.54, NivelAfluencia.Bajo, 5.0, 5);

        assertEquals(1, plan.size());
        assertEquals("1", plan.get(0).getComercioId());
    }

    @Test
    void generarPlan_FiltraPorNivelDeAfluencia() {
        when(comercioRepository.findByEstadoAprobacionAndUbicacionNear(eq(EstadoComercio.APROBADO), any(), any()))
                .thenReturn(List.of(comercio1));
        
        // Simular ALTA afluencia (9 puntos + cerca)
        List<PuntoMapaCalorDTO> puntosCalorAlta = Arrays.asList(
                new PuntoMapaCalorDTO(10.42, -75.54, 1.0), new PuntoMapaCalorDTO(10.42, -75.54, 1.0),
                new PuntoMapaCalorDTO(10.42, -75.54, 1.0), new PuntoMapaCalorDTO(10.42, -75.54, 1.0),
                new PuntoMapaCalorDTO(10.42, -75.54, 1.0), new PuntoMapaCalorDTO(10.42, -75.54, 1.0),
                new PuntoMapaCalorDTO(10.42, -75.54, 1.0), new PuntoMapaCalorDTO(10.42, -75.54, 1.0),
                new PuntoMapaCalorDTO(10.42, -75.54, 1.0)
        );
        when(heatmapService.obtenerPuntosCalorTiempoReal()).thenReturn(puntosCalorAlta);

        // Pedimos BAJA
        List<ComercioPlanDTO> planBaja = planInteligenteService.generarPlanDinamica(
                10.41, -75.54, NivelAfluencia.Bajo, 10.0, 5);
        
        assertTrue(planBaja.isEmpty(), "No debería retornar el comercio 1 porque su afluencia es ALTA y se pidió BAJA");

        // Pedimos ALTA
        List<ComercioPlanDTO> planAlta = planInteligenteService.generarPlanDinamica(
                10.41, -75.54, NivelAfluencia.Alto, 10.0, 5);

        assertEquals(1, planAlta.size(), "Debería retornar el comercio 1 ya que empata con ALTA");
    }

    @Test
    void generarPlan_DevuelveListaVaciaSiNoAplicaNinguno() {
        when(comercioRepository.findByEstadoAprobacionAndUbicacionNear(eq(EstadoComercio.APROBADO), any(), any()))
                .thenReturn(Collections.emptyList());
        when(heatmapService.obtenerPuntosCalorTiempoReal()).thenReturn(Collections.emptyList());

        List<ComercioPlanDTO> plan = planInteligenteService.generarPlanDinamica(
                10.41, -75.54, NivelAfluencia.Bajo, 10.0, 5);

        assertNotNull(plan);
        assertTrue(plan.isEmpty());
    }

    @Test
    void generarPlan_LimitaCantidadMaximaDeParadas() {
        when(comercioRepository.findByEstadoAprobacionAndUbicacionNear(eq(EstadoComercio.APROBADO), any(), any()))
                .thenReturn(Arrays.asList(comercio1, comercio2));
        when(heatmapService.obtenerPuntosCalorTiempoReal()).thenReturn(Collections.emptyList()); // BAJA

        // Solicitamos máximo 1 parada a pesar de haber 2 disponibles
        List<ComercioPlanDTO> plan = planInteligenteService.generarPlanDinamica(
                10.41, -75.54, NivelAfluencia.Bajo, 10.0, 1);

        assertEquals(1, plan.size(), "Debe aplicar el límite de maxParadas");
        assertEquals("1", plan.get(0).getComercioId());
    }
}
