package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.EstadoComercio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.geo.Distance;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio de MongoDB para la entidad Comercio.
 * Proporciona métodos para filtrar comercios por zona, tipo, estado de
 * aprobación
 * y consultas geoespaciales (Near) para mapas.
 */
@Repository
public interface ComercioRepository extends MongoRepository<Comercio, String> {

        // Para la función: "Mostrar todos los comercios de ESTA zona"
        List<Comercio> findByZonaIdAndEstadoAprobacion(String zonaId, EstadoComercio estadoAprobacion);

        // Para la función: "Filtrar por tipo de negocio"
        List<Comercio> findByTipoNegocioIdAndEstadoAprobacion(Long tipoNegocioId, EstadoComercio estadoAprobacion);

        // Para el filtro combinado: "Mostrar restaurantes en ESTA zona"
        List<Comercio> findByZonaIdAndTipoNegocioIdAndEstadoAprobacion(String zonaId, Long tipoNegocioId,
                        EstadoComercio estadoAprobacion);

        // Para listar pendientes o aprobados en general (paginado — panel admin)
        Page<Comercio> findByEstadoAprobacion(EstadoComercio estadoAprobacion, Pageable pageable);

        // Para buscar por nombre dentro de un estado
        Page<Comercio> findByEstadoAprobacionAndNombreContainingIgnoreCase(EstadoComercio estadoAprobacion,
                        String nombre,
                        Pageable pageable);

        // Búsqueda combinada: Por nombre o por dirección (ignorando mayúsculas)
        // manteniendo el filtro de estado
        @org.springframework.data.mongodb.repository.Query("{ 'estadoAprobacion': ?0, $or: [ { 'nombre': { $regex: ?1, $options: 'i' } }, { 'direccion': { $regex: ?1, $options: 'i' } } ] }")
        Page<Comercio> buscarPorEstadoYTerminoCombinado(EstadoComercio estadoAprobacion, String terminoBusqueda,
                        Pageable pageable);

        // Para verificar comercios asociados a un propietario
        List<Comercio> findByPropietarioIdAndEstadoAprobacionIn(String propietarioId, List<EstadoComercio> estados);

        List<Comercio> findByPropietarioId(String propietarioId);

        // Eliminar todos los comercios asociados a un usuario eliminado
        void deleteByPropietarioId(String propietarioId);

        /**
         * Lista sin paginación de comercios por estado.
         * Usado por PlanInteligenteServiceImpl para obtener solo comercios APROBADO.
         */
        List<Comercio> findByEstadoAprobacion(EstadoComercio estadoAprobacion);

        // Búsqueda por cercanía para PlanInteligente
        List<Comercio> findByEstadoAprobacionAndUbicacionNear(EstadoComercio estadoAprobacion, GeoJsonPoint point,
                        Distance distance);
}