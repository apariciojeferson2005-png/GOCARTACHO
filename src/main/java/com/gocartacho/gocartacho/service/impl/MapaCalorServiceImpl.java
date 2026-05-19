package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.dto.PuntoMapaCalorDTO;
import com.gocartacho.gocartacho.model.*;
import com.gocartacho.gocartacho.repository.AfluenciaHistoricaRepository;
import com.gocartacho.gocartacho.repository.PuntoCalorRepository;
import com.gocartacho.gocartacho.repository.ZonaRepository;
import com.gocartacho.gocartacho.service.MapaCalorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación de la lógica de negocio para MapaCalor.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapaCalorServiceImpl implements MapaCalorService {

    private final PuntoCalorRepository puntoCalorRepository;
    private final AfluenciaHistoricaRepository afluenciaHistoricaRepository;
    private final ZonaRepository zonaRepository;

    // Constantes para evitar "números mágicos"
    private static final int RANGO_TIEMPO_REAL_HORAS = 2;
    private static final int RANGO_BACKUP_HORAS = 24;

    @Override
    public void guardarPuntoCalor(PuntoCalor nuevoPunto) {
        if (nuevoPunto == null || nuevoPunto.getDispositivoHash() == null) {
            log.warn("Se intentó guardar un punto de calor nulo o sin identificador de dispositivo.");
            return;
        }

        // Buscar si ya existe un punto para este dispositivo para actualizarlo (Upsert)
        // Esto mantiene la BD optimizada: 1 registro por usuario activo.
        Optional<PuntoCalor> puntoExistente = puntoCalorRepository
                .findByDispositivoHash(nuevoPunto.getDispositivoHash());

        PuntoCalor puntoAGuardar;
        if (puntoExistente.isPresent()) {
            puntoAGuardar = puntoExistente.get();
            puntoAGuardar.setLatitud(nuevoPunto.getLatitud());
            puntoAGuardar.setLongitud(nuevoPunto.getLongitud());
            puntoAGuardar.setUbicacion(nuevoPunto.getUbicacion());
            puntoAGuardar.setTimestamp(LocalDateTime.now());
            log.debug("Actualizando punto de calor para dispositivo: {}", nuevoPunto.getDispositivoHash());
        } else {
            puntoAGuardar = nuevoPunto;
            puntoAGuardar.setTimestamp(LocalDateTime.now());
            log.debug("Nuevo punto de calor para dispositivo: {}", nuevoPunto.getDispositivoHash());
        }

        puntoCalorRepository.save(puntoAGuardar);
    }

    @Override
    @Cacheable(value = "heatmap_live", cacheManager = "shortLivedCacheManager")
    public List<PuntoMapaCalorDTO> obtenerPuntosCalorTiempoReal() {
        LocalDateTime ahora = LocalDateTime.now();

        // 1. Intentar obtener datos de las últimas 2 horas
        List<PuntoCalor> puntos = puntoCalorRepository.findByTimestampAfter(ahora.minusHours(RANGO_TIEMPO_REAL_HORAS));

        // 2. Fallback a 24 horas si no hay actividad reciente
        if (puntos.isEmpty()) {
            log.info("No hay puntos recientes (2h), buscando en las últimas 24h");
            puntos = puntoCalorRepository.findByTimestampAfter(ahora.minusHours(RANGO_BACKUP_HORAS));
        }

        // 3. Auto-regeneración para desarrollo (Evita que el mapa se vacíe por el índice TTL de MongoDB)
        if (puntos.isEmpty()) {
            log.info("El mapa de calor está vacío. Generando tráfico simulado automáticamente...");
            generarTraficoSimulado();
            puntos = puntoCalorRepository.findByTimestampAfter(ahora.minusHours(RANGO_TIEMPO_REAL_HORAS));
        }

        // 4. Agregación en Rejilla (Grid) de ~111 metros (0.001 grados)
        double cellSizeDeg = 0.001;
        Map<String, List<PuntoCalor>> grid = new HashMap<>();
        for (PuntoCalor p : puntos) {
            if (p.getLatitud() == null || p.getLongitud() == null)
                continue;
            int cellX = (int) Math.floor(p.getLongitud().doubleValue() / cellSizeDeg);
            int cellY = (int) Math.floor(p.getLatitud().doubleValue() / cellSizeDeg);
            String key = cellX + "," + cellY;
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

        // 4. Calcular centroides y densidad
        List<PuntoMapaCalorDTO> resultados = new ArrayList<>();
        for (List<PuntoCalor> puntosEnCelda : grid.values()) {
            double sumLat = 0;
            double sumLng = 0;
            for (PuntoCalor p : puntosEnCelda) {
                sumLat += p.getLatitud().doubleValue();
                sumLng += p.getLongitud().doubleValue();
            }
            int cantidad = puntosEnCelda.size();
            resultados.add(new PuntoMapaCalorDTO(
                    sumLat / cantidad,
                    sumLng / cantidad,
                    (double) cantidad // La intensidad es exactamente la cantidad de usuarios en este radio
            ));
        }

        return resultados;
    }

    @Override
    public NivelAfluencia obtenerAfluenciaHistorica(String zonaId, DiaSemana dia, int hora) {
        if (zonaId == null || dia == null) {
            return NivelAfluencia.Bajo;
        }

        // Verificamos si la zona existe en MongoDB antes de buscar en histórico
        if (!zonaRepository.existsById(zonaId)) {
            log.error("Se intentó buscar afluencia para una zona inexistente: ID {}", zonaId);
            return NivelAfluencia.Bajo;
        }

        return afluenciaHistoricaRepository
                .findByZonaIdAndDiaSemanaAndHora(zonaId, dia, hora)
                .map(AfluenciaHistorica::getNivelPromedio)
                .orElse(NivelAfluencia.Bajo);
    }

    @Override
    public List<AfluenciaHistorica> obtenerHistorialPorZona(String zonaId) {
        if (zonaId == null || !zonaRepository.existsById(zonaId)) {
            return new ArrayList<>();
        }
        return afluenciaHistoricaRepository.findByZonaId(zonaId);
    }

    // El método convertirADto fue eliminado ya que ahora la agregación se encarga
    // de crear el DTO directamente.

    private void generarTraficoSimulado() {
        List<Zona> zonas = zonaRepository.findAll();
        int devIndex = 1;
        for (Zona z : zonas) {
            if (z.getLatitud() != null && z.getLongitud() != null) {
                // Generar entre 10 y 30 usuarios por zona
                int numPuntos = 10 + (int) (Math.random() * 20);
                for (int i = 0; i < numPuntos; i++) {
                    PuntoCalor pc = new PuntoCalor();
                    // Dispersión aleatoria realista (+/- 800 metros alrededor del centro de la zona)
                    double latJitter = (Math.random() - 0.5) * 0.015;
                    double lngJitter = (Math.random() - 0.5) * 0.015;
                    pc.setLatitud(z.getLatitud().add(java.math.BigDecimal.valueOf(latJitter)));
                    pc.setLongitud(z.getLongitud().add(java.math.BigDecimal.valueOf(lngJitter)));
                    pc.setUbicacion(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(
                            pc.getLongitud().doubleValue(), pc.getLatitud().doubleValue()));
                    pc.setTimestamp(LocalDateTime.now());
                    pc.setDispositivoHash("simulado_" + devIndex++);
                    puntoCalorRepository.save(pc);
                }
            }
        }
    }
}
