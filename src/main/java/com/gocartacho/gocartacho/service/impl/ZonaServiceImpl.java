package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.AfluenciaHistorica;
import com.gocartacho.gocartacho.model.DiaSemana;
import com.gocartacho.gocartacho.model.Zona;
import com.gocartacho.gocartacho.repository.ZonaRepository;
import com.gocartacho.gocartacho.repository.AfluenciaHistoricaRepository;
import com.gocartacho.gocartacho.service.ZonaService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ZonaServiceImpl implements ZonaService {

    private final ZonaRepository zonaRepository;
    private final AfluenciaHistoricaRepository afluenciaRepository;

    /**
     * FIX N+1: En lugar de hacer una query a MongoDB por cada zona, se obtienen todos los registros
     * de afluencia para todas las zonas en una sola consulta bulk y luego se mapean en memoria.
     */
    @Override
    @Cacheable(value = "zonas_static", cacheManager = "longLivedCacheManager")
    public List<Zona> obtenerTodasLasZonas() {
        LocalDateTime ahora = LocalDateTime.now();
        int horaActual = ahora.getHour();
        DiaSemana diaActual = mapearDiaSemana(ahora.getDayOfWeek());

        List<Zona> zonas = zonaRepository.findAll();

        if (diaActual == null || zonas.isEmpty()) {
            return zonas;
        }

        // Obtener IDs de todas las zonas para la consulta bulk
        List<String> zonaIds = zonas.stream()
                .map(Zona::getZonaId)
                .collect(Collectors.toList());

        // Una sola query a MongoDB para todas las zonas a la vez
        List<AfluenciaHistorica> afluencias = afluenciaRepository
                .findByZonaIdInAndDiaSemanaAndHora(zonaIds, diaActual, horaActual);

        // Construir mapa zonaId → nivel para lookup O(1)
        Map<String, Integer> nivelPorZona = afluencias.stream()
                .collect(Collectors.toMap(
                        AfluenciaHistorica::getZonaId,
                        a -> a.getNivelPromedio().ordinal(),
                        (existing, replacement) -> existing // en caso de duplicados, conservar el primero
                ));

        // Enriquecer cada zona con su nivel de afluencia (lookup en memoria, sin más queries)
        zonas.forEach(zona -> {
            Integer nivel = nivelPorZona.get(zona.getZonaId());
            if (nivel != null) {
                zona.setNivelConcurrencia(nivel);
            }
        });

        return zonas;
    }

    @Override
    public Zona obtenerZonaPorId(String id) {
        if (id == null) return null;

        return zonaRepository.findById(id)
                .map(this::enriquecerZonaConAfluencia)
                .orElse(null);
    }

    @Override
    public Zona obtenerZonaPorNumero(Integer numero) {
        if (numero == null) return null;
        return zonaRepository.findByNumero(numero)
                .map(this::enriquecerZonaConAfluencia)
                .orElse(null);
    }

    /** Enriquece una zona individual con su afluencia (usado solo en obtenerZonaPorId). */
    private Zona enriquecerZonaConAfluencia(Zona zona) {
        LocalDateTime ahora = LocalDateTime.now();
        int horaActual = ahora.getHour();
        DiaSemana diaActual = mapearDiaSemana(ahora.getDayOfWeek());

        if (diaActual == null) {
            return zona;
        }

        afluenciaRepository.findByZonaIdAndDiaSemanaAndHora(zona.getZonaId(), diaActual, horaActual)
                .ifPresent(afluencia -> zona.setNivelConcurrencia(afluencia.getNivelPromedio().ordinal()));

        return zona;
    }

    /** Convierte java.time.DayOfWeek (inglés) a DiaSemana (español). */
    private DiaSemana mapearDiaSemana(java.time.DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:    return DiaSemana.Lunes;
            case TUESDAY:   return DiaSemana.Martes;
            case WEDNESDAY: return DiaSemana.Miércoles;
            case THURSDAY:  return DiaSemana.Jueves;
            case FRIDAY:    return DiaSemana.Viernes;
            case SATURDAY:  return DiaSemana.Sábado;
            case SUNDAY:    return DiaSemana.Domingo;
            default:        return null;
        }
    }

    @Override
    @Transactional
    public Zona guardarZona(Zona zona) {
        if (zona == null) throw new IllegalArgumentException("La zona no puede ser nula.");
        return zonaRepository.save(zona);
    }

    @Override
    @Transactional
    public void eliminarZona(String id) {
        if (id == null || !zonaRepository.existsById(id)) {
            throw new IllegalArgumentException("ID inválido o zona inexistente.");
        }
        zonaRepository.deleteById(id);
    }
}