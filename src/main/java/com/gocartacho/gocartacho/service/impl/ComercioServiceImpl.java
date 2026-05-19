package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.EstadoComercio;
import com.gocartacho.gocartacho.model.Zona;
import com.gocartacho.gocartacho.repository.ComercioRepository;
import com.gocartacho.gocartacho.repository.ZonaRepository;
import com.gocartacho.gocartacho.service.ComercioService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de la lógica de negocio para Comercio.
 */
@Service
@Transactional(readOnly = true)
public class ComercioServiceImpl implements ComercioService {

    private final ComercioRepository comercioRepository;
    private final ZonaRepository zonaRepository;
    private final com.gocartacho.gocartacho.service.TipoNegocioService tipoNegocioService;

    public ComercioServiceImpl(ComercioRepository comercioRepository, ZonaRepository zonaRepository,
            com.gocartacho.gocartacho.service.TipoNegocioService tipoNegocioService) {
        this.comercioRepository = comercioRepository;
        this.zonaRepository = zonaRepository;
        this.tipoNegocioService = tipoNegocioService;
    }

    @Override
    @Cacheable(value = "comercios", key = "#terminoBusqueda + '-' + #pageable.pageNumber", unless = "#result.totalElements == 0")
    public Page<Comercio> obtenerTodosLosComercios(String terminoBusqueda, Pageable pageable) {
        Page<Comercio> page;
        if (terminoBusqueda == null || terminoBusqueda.isBlank()) {
            page = comercioRepository.findByEstadoAprobacion(EstadoComercio.APROBADO, pageable);
        } else {
            page = comercioRepository.buscarPorEstadoYTerminoCombinado(EstadoComercio.APROBADO, terminoBusqueda,
                    pageable);
        }
        enriquecerNombresTipo(page.getContent());
        return page;
    }

    @Override
    public Page<Comercio> obtenerComerciosPorEstado(EstadoComercio estado, String terminoBusqueda, Pageable pageable) {
        Page<Comercio> page;
        if (terminoBusqueda == null || terminoBusqueda.isBlank()) {
            page = comercioRepository.findByEstadoAprobacion(estado, pageable);
        } else {
            page = comercioRepository.buscarPorEstadoYTerminoCombinado(estado, terminoBusqueda, pageable);
        }
        enriquecerNombresTipo(page.getContent());
        return page;
    }

    @Override
    public List<Comercio> obtenerComerciosPorPropietarioYEstados(String propietarioId,
            java.util.List<EstadoComercio> estados) {
        if (propietarioId == null || estados == null || estados.isEmpty()) {
            return Collections.emptyList();
        }
        List<Comercio> comercios = comercioRepository.findByPropietarioIdAndEstadoAprobacionIn(propietarioId, estados);
        enriquecerNombresTipo(comercios);
        return comercios;
    }

    @Override
    public Comercio obtenerComercioPorId(String id) {
        if (id == null) {
            return null;
        }
        Comercio comercio = comercioRepository.findById(id).orElse(null);
        if (comercio != null) {
            if (comercio.getTipoNegocioId() != null) {
                tipoNegocioService.obtenerPorId(comercio.getTipoNegocioId())
                        .ifPresent(tipo -> comercio.setTipoNegocioNombre(tipo.getNombre()));
            }
            String zonaId = comercio.getZonaId();
            if (zonaId != null) {
                zonaRepository.findById(zonaId)
                        .ifPresent(zona -> comercio.setZonaNombre(zona.getNombre()));
            }
        }
        return comercio;
    }

    @Override
    @Transactional
    @CacheEvict(value = "comercios", allEntries = true)
    public Comercio guardarComercio(Comercio comercio) {
        if (comercio == null) {
            throw new IllegalArgumentException("El objeto comercio no puede ser nulo.");
        }
        if (comercio.getEstadoAprobacion() == null) {
            comercio.setEstadoAprobacion(EstadoComercio.PENDIENTE);
        }

        // Sincronizar ubicacion para índices geoespaciales
        if (comercio.getLatitud() != null && comercio.getLongitud() != null) {
            comercio.setUbicacion(new GeoJsonPoint(
                    comercio.getLongitud().doubleValue(),
                    comercio.getLatitud().doubleValue()));
        }

        return comercioRepository.save(comercio);
    }

    @Override
    @Cacheable(value = "comercios", key = "'zona-' + #zonaId")
    public List<Comercio> obtenerComerciosPorZona(String zonaId) {
        if (zonaId == null) {
            return Collections.emptyList();
        }

        Optional<Zona> zona = zonaRepository.findById(zonaId);

        List<Comercio> comercios = zona
                .map(z -> comercioRepository.findByZonaIdAndEstadoAprobacion(zonaId, EstadoComercio.APROBADO))
                .orElse(Collections.emptyList());
        enriquecerNombresTipo(comercios);
        return comercios;
    }

    @Override
    public List<Comercio> obtenerComerciosPorZonaYTipo(String zonaId, Long tipoId) {
        if (zonaId == null || tipoId == null) {
            return Collections.emptyList();
        }

        Optional<Zona> zona = zonaRepository.findById(zonaId);

        List<Comercio> comercios = zona
                .map(z -> comercioRepository.findByZonaIdAndTipoNegocioIdAndEstadoAprobacion(zonaId, tipoId,
                        EstadoComercio.APROBADO))
                .orElse(Collections.emptyList());

        enriquecerNombresTipo(comercios);

        return comercios;
    }

    @Override
    public long contarComercios() {
        return comercioRepository.count();
    }

    @Override
    public List<Comercio> obtenerTodosSinFiltro() {
        List<Comercio> list = comercioRepository.findAll();
        enriquecerNombresTipo(list);
        return list;
    }

    @Override
    @Transactional
    @CacheEvict(value = "comercios", allEntries = true)
    public void eliminarComercio(String id) {
        if (id != null) {
            comercioRepository.deleteById(id);
        }
    }

    /**
     * Enriquece una lista de comercios con el nombre del tipo de negocio desde
     * MySQL y el nombre de la zona desde MongoDB.
     * Optimizado para evitar el problema de consultas N+1.
     */
    private void enriquecerNombresTipo(List<Comercio> comercios) {
        if (comercios == null || comercios.isEmpty())
            return;

        // 1. Enriquecer Tipo de Negocio Nombre
        java.util.Set<Long> idsTipos = comercios.stream()
                .map(Comercio::getTipoNegocioId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (!idsTipos.isEmpty()) {
            java.util.Map<Long, String> mapaNombres = tipoNegocioService.listarTodos().stream()
                    .filter(tn -> idsTipos.contains(tn.getId()))
                    .collect(java.util.stream.Collectors.toMap(
                            com.gocartacho.gocartacho.model.TipoNegocio::getId,
                            com.gocartacho.gocartacho.model.TipoNegocio::getNombre));

            comercios.forEach(c -> {
                if (c.getTipoNegocioId() != null) {
                    c.setTipoNegocioNombre(mapaNombres.get(c.getTipoNegocioId()));
                }
            });
        }

        // 2. Enriquecer Zona Nombre
        java.util.Set<String> idsZonas = comercios.stream()
                .map(Comercio::getZonaId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (!idsZonas.isEmpty()) {
            java.util.List<Zona> zonas = zonaRepository.findAllById(idsZonas);
            java.util.Map<String, String> mapaZonas = zonas.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Zona::getZonaId,
                            Zona::getNombre));

            comercios.forEach(c -> {
                if (c.getZonaId() != null) {
                    c.setZonaNombre(mapaZonas.get(c.getZonaId()));
                }
            });
        }
    }
}
