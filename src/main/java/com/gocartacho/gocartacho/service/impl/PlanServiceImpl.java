package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.dto.ComercioDTO;
import com.gocartacho.gocartacho.dto.PuntoMapaCalorDTO;
import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.Plan;
import com.gocartacho.gocartacho.model.PlanComercio;
import com.gocartacho.gocartacho.repository.ComercioRepository;
import com.gocartacho.gocartacho.repository.PlanComercioRepository;
import com.gocartacho.gocartacho.repository.PlanRepository;
import com.gocartacho.gocartacho.repository.PuntoCalorRepository;
import com.gocartacho.gocartacho.service.MapaCalorService;
import com.gocartacho.gocartacho.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Objects;
import java.time.LocalDateTime;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

/**
 * Implementación de la lógica de negocio para Plan.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final PlanComercioRepository planComercioRepository;
    private final ComercioRepository comercioRepository;
    private final MapaCalorService mapaCalorService;
    private final com.gocartacho.gocartacho.service.TipoNegocioService tipoNegocioService;
    private final PuntoCalorRepository puntoCalorRepository;

    @Override
    public List<Plan> obtenerTodasLasPlanes() {
        return planRepository.findAll();
    }

    @Override
    public Optional<Plan> obtenerPlanPorId(String planId) {
        return Optional.ofNullable(planId).flatMap(planRepository::findById);
    }

    @Override
    public Map<String, Integer> calcularAfluenciaParaPlanes(List<String> planesIds) {
        if (planesIds == null || planesIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<PuntoMapaCalorDTO> puntosActuales = mapaCalorService.obtenerPuntosCalorTiempoReal();

        return planesIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> calcularTotalUsuariosEnPlan(id, puntosActuales)));
    }

    private Integer calcularTotalUsuariosEnPlan(String planId, List<PuntoMapaCalorDTO> puntosActuales) {
        return obtenerComerciosPorPlan(planId, puntosActuales).stream()
                .mapToInt(ComercioDTO::getUsuariosActuales)
                .sum();
    }

    @Override
    public List<ComercioDTO> obtenerComerciosPorPlan(String planId) {
        List<PuntoMapaCalorDTO> puntosActuales = mapaCalorService.obtenerPuntosCalorTiempoReal();
        return obtenerComerciosPorPlan(planId, puntosActuales);
    }

    @SuppressWarnings("null")
    private List<ComercioDTO> obtenerComerciosPorPlan(String planId, List<PuntoMapaCalorDTO> puntosActuales) {
        if (planId == null) {
            return Collections.emptyList();
        }

        Optional<Plan> planOpt = planRepository.findById(planId);

        if (planOpt.isEmpty()) {
            return Collections.emptyList();
        }

        Plan plan = planOpt.get();
        List<PlanComercio> uniones = planComercioRepository.findByPlanIdOrderByOrdenAsc(plan.getPlanId());

        List<String> comercioIds = uniones.stream()
                .map(PlanComercio::getComercioId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Comercio> comerciosMap = comercioRepository.findAllById(comercioIds).stream()
                .collect(Collectors.toMap(Comercio::getComercioId, c -> c));

        List<Comercio> comercios = uniones.stream()
                .map(union -> comerciosMap.get(union.getComercioId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Cargar nombres de tipos en bloque
        Map<Long, String> mapaNombres = new HashMap<>();
        java.util.Set<Long> idsTipos = comercios.stream()
                .map(Comercio::getTipoNegocioId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!idsTipos.isEmpty()) {
            mapaNombres = tipoNegocioService.listarTodos().stream()
                    .filter(tn -> idsTipos.contains(tn.getId()))
                    .collect(Collectors.toMap(
                            com.gocartacho.gocartacho.model.TipoNegocio::getId,
                            com.gocartacho.gocartacho.model.TipoNegocio::getNombre));
        }

        Map<Long, String> finalMapaNombres = mapaNombres;

        // Fecha límite para considerar un usuario como "activo" (últimas 2 horas)
        LocalDateTime fechaLimite = LocalDateTime.now().minusHours(2);

        return comercios.stream()
                .map(c -> {
                    long usuariosReales = 0;
                    if (c.getLatitud() != null && c.getLongitud() != null) {
                        GeoJsonPoint ubicacionComercio = new GeoJsonPoint(c.getLongitud().doubleValue(),
                                c.getLatitud().doubleValue());
                        usuariosReales = puntoCalorRepository.countByUbicacionNearAndTimestampAfter(
                                ubicacionComercio,
                                new Distance(0.12, Metrics.KILOMETERS), // Radio de 120 metros
                                fechaLimite);
                    }

                    String recomendacion = uniones.stream()
                            .filter(u -> u.getComercioId().equals(c.getComercioId()))
                            .findFirst()
                            .map(com.gocartacho.gocartacho.model.PlanComercio::getRecomendacion)
                            .orElse(null);

                    ComercioDTO dto = new ComercioDTO(
                            c.getComercioId(),
                            c.getNombre(),
                            c.getTipoNegocioId(),
                            finalMapaNombres.get(c.getTipoNegocioId()),
                            (int) usuariosReales);
                    dto.setLatitud(c.getLatitud());
                    dto.setLongitud(c.getLongitud());
                    dto.setRecomendacion(recomendacion);
                    dto.setPromedioCalificacion(c.getPromedioCalificacion());
                    dto.setTotalResenas(c.getTotalResenas());
                    dto.setDireccion(c.getDireccion());
                    dto.setDescripcion(c.getDescripcion());
                    dto.setTelefono(c.getTelefono());
                    dto.setImagenUrl(c.getImagenUrl());
                    return dto;
                })
                .collect(Collectors.toList());
    }


}
