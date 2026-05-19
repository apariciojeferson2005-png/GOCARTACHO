package com.gocartacho.gocartacho.service;

import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.EstadoComercio;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define las operaciones de negocio para Comercio.
 */
public interface ComercioService {

    Page<Comercio> obtenerTodosLosComercios(String terminoBusqueda, Pageable pageable);

    Comercio obtenerComercioPorId(String id);

    Comercio guardarComercio(Comercio comercio);

    List<Comercio> obtenerComerciosPorZona(String zonaId);

    List<Comercio> obtenerComerciosPorZonaYTipo(String zonaId, Long tipoId);

    Page<Comercio> obtenerComerciosPorEstado(EstadoComercio estado, String terminoBusqueda, Pageable pageable);

    List<Comercio> obtenerComerciosPorPropietarioYEstados(String propietarioId, java.util.List<EstadoComercio> estados);

    long contarComercios();

    java.util.List<Comercio> obtenerTodosSinFiltro();

    void eliminarComercio(String id);
}
