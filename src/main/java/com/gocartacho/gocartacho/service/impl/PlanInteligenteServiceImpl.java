package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.config.HeatmapProperties;
import com.gocartacho.gocartacho.dto.ComercioPlanDTO;
import com.gocartacho.gocartacho.dto.PuntoMapaCalorDTO;
import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.EstadoComercio;
import com.gocartacho.gocartacho.model.NivelAfluencia;
import com.gocartacho.gocartacho.repository.ComercioRepository;
import com.gocartacho.gocartacho.service.MapaCalorService;
import com.gocartacho.gocartacho.service.PlanInteligenteService;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class PlanInteligenteServiceImpl implements PlanInteligenteService {

    private final ComercioRepository comercioRepository;
    private final MapaCalorService heatmapService;
    private final com.gocartacho.gocartacho.service.TipoNegocioService tipoNegocioService;

    private final HeatmapProperties heatmapProperties;

    @Override
    public List<ComercioPlanDTO> generarPlanDinamica(double latUsuario, double lonUsuario,
            NivelAfluencia afluenciaDeseada, double radioKm, int maxParadas) {
        try {
            // 1. Obtener solo comercios APROBADOS que estén en el radio de el plan
        // Optimizamos usando búsqueda geoespacial de MongoDB en lugar de cargar todo en memoria
        List<Comercio> candidatosRadio = comercioRepository.findByEstadoAprobacionAndUbicacionNear(
                EstadoComercio.APROBADO, 
                new GeoJsonPoint(lonUsuario, latUsuario), 
                new Distance(radioKm, Metrics.KILOMETERS)
        );
        
        List<PuntoMapaCalorDTO> puntosCalor = heatmapService.obtenerPuntosCalorTiempoReal();
        if (puntosCalor == null) {
            puntosCalor = new ArrayList<>();
        }

        // 2. Pre-procesar puntos de calor en una Rejilla (Grid) para optimizar de
        // O(N*M) a O(N+M)
        double cellSizeDeg = 0.001; // Aprox 111 metros por celda
        Map<String, List<PuntoMapaCalorDTO>> grid = new HashMap<>();
        for (PuntoMapaCalorDTO p : puntosCalor) {
            // FIX: Usar Math.floor para coordenadas negativas (Cartagena está en el hemisferio occidental)
            int cellX = (int) Math.floor(p.getLng() / cellSizeDeg);
            int cellY = (int) Math.floor(p.getLat() / cellSizeDeg);
            String key = cellX + "," + cellY;
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

            // Buscar solo en las celdas directamente colindantes según el radio configurado
            double radius = heatmapProperties.getBusinessRadiusKm();
            int celdasBusqueda = (int) Math.ceil(radius / 0.111);

            // 3. Filtrar candidatos por Afluencia en Tiempo Real usando la Rejilla
            List<Comercio> candidatosFinales = new ArrayList<>();
            for (Comercio c : candidatosRadio) {
                if (c.getLatitud() == null || c.getLongitud() == null)
                    continue;
                
                double latC = c.getLatitud().doubleValue();
                double lonC = c.getLongitud().doubleValue();

                int centerCellX = (int) Math.floor(lonC / cellSizeDeg);
                int centerCellY = (int) Math.floor(latC / cellSizeDeg);
                long genteCerca = 0;

                for (int i = -celdasBusqueda; i <= celdasBusqueda; i++) {
                    for (int j = -celdasBusqueda; j <= celdasBusqueda; j++) {
                        String key = (centerCellX + i) + "," + (centerCellY + j);
                        List<PuntoMapaCalorDTO> puntosEnCelda = grid.get(key);
                        if (puntosEnCelda != null) {
                            for (PuntoMapaCalorDTO p : puntosEnCelda) {
                                if (calcularDistancia(p.getLat(), p.getLng(), latC, lonC) <= radius) {
                                    genteCerca++;
                                }
                            }
                        }
                    }
                }

                NivelAfluencia nivelActual = determinarNivelAfluencia(genteCerca);
                if (nivelActual == afluenciaDeseada) {
                    candidatosFinales.add(c);
                }
            }

            // 4. Generar el plan usando el Algoritmo del Vecino Más Cercano (Nearest Neighbor)
            return construirPlanNearestNeighbor(candidatosFinales, latUsuario, lonUsuario, maxParadas);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Error generando plan inteligente: {}", e.getMessage(), e);
            return new ArrayList<>(); // Devolver lista vacía en lugar de 500
        }
    }

    // --- MÉTODOS DE APOYO PARA GENERAR PLAN ---

    private List<ComercioPlanDTO> construirPlanNearestNeighbor(List<Comercio> candidatos, double latUsuario,
            double lonUsuario, int maxParadas) {
        List<ComercioPlanDTO> planFinal = new ArrayList<>();
        double currentLat = latUsuario;
        double currentLon = lonUsuario;

        // Pre-cargar nombres de tipos de negocio para evitar N+1 en el bucle
        java.util.Map<Long, String> mapaNombres = new java.util.HashMap<>();
        java.util.Set<Long> idsTipos = candidatos.stream()
                .map(Comercio::getTipoNegocioId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (!idsTipos.isEmpty()) {
            mapaNombres = tipoNegocioService.listarTodos().stream()
                    .filter(tn -> idsTipos.contains(tn.getId()))
                    .collect(java.util.stream.Collectors.toMap(
                            com.gocartacho.gocartacho.model.TipoNegocio::getId,
                            com.gocartacho.gocartacho.model.TipoNegocio::getNombre
                    ));
        }

        while (!candidatos.isEmpty() && planFinal.size() < maxParadas) {
            Comercio masCercano = null;
            double minDistancia = Double.MAX_VALUE;

            for (Comercio c : candidatos) {
                double dist = calcularDistancia(currentLat, currentLon, c.getLatitud().doubleValue(),
                        c.getLongitud().doubleValue());
                if (dist < minDistancia) {
                    minDistancia = dist;
                    masCercano = c;
                }
            }

            if (masCercano != null) {
                ComercioPlanDTO dto = new ComercioPlanDTO(
                        masCercano.getComercioId(),
                        masCercano.getNombre(),
                        masCercano.getTipoNegocioId(),
                        mapaNombres.get(masCercano.getTipoNegocioId()),
                        masCercano.getLatitud(),
                        masCercano.getLongitud());
                dto.setDistanciaDesdeAnteriorKm(minDistancia);
                planFinal.add(dto);

                // Actualizar punto actual y remover de la lista
                currentLat = masCercano.getLatitud().doubleValue();
                currentLon = masCercano.getLongitud().doubleValue();
                candidatos.remove(masCercano);
            } else {
                break; // Prevenir un ciclo infinito si no hay candidatos válidos restantes
            }
        }

        return planFinal;
    }

    // --- UTILIDADES ---

    /**
     * Fórmula de Haversine para calcular distancia en línea recta en Kilómetros
     */
    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Lógica de negocio para traducir cantidad de pings a un String amigable
     */
    private NivelAfluencia determinarNivelAfluencia(long cantidadPings) {
        if (cantidadPings <= heatmapProperties.getThreshold().getLow()) {
            return NivelAfluencia.Bajo;
        } else if (cantidadPings <= heatmapProperties.getThreshold().getMedium()) {
            return NivelAfluencia.Medio;
        } else {
            return NivelAfluencia.Alto;
        }
    }
}