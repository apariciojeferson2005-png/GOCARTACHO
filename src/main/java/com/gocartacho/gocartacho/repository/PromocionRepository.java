package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.Promocion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad Promocion en MongoDB.
 */
@Repository
public interface PromocionRepository extends MongoRepository<Promocion, String> {

    List<Promocion> findByComercioId(String comercioId);

    // Promociones activas (fecha actual entre inicio y fin, y activa=true)
    @Query("{ 'activa': true, 'fecha_inicio': { $lte: ?0 }, 'fecha_fin': { $gte: ?0 } }")
    List<Promocion> findPromocionesActivas(LocalDate hoy);

    // Promociones activas por zona (Requiere un paso extra en servicio o aggregation, pero lo simplificamos a comercioId si no hay agregación)
    // Para simplificar, la búsqueda por zona se puede hacer obteniendo los comercios de la zona primero en el servicio
    // y luego usando findByComercioIdIn
    @Query("{ 'activa': true, 'fecha_inicio': { $lte: ?0 }, 'fecha_fin': { $gte: ?0 }, 'comercio_id': { $in: ?1 } }")
    List<Promocion> findPromocionesActivasPorComercios(LocalDate hoy, List<String> comercioIds);

    // Promociones de un comercio específico activas
    @Query("{ 'activa': true, 'fecha_inicio': { $lte: ?0 }, 'fecha_fin': { $gte: ?0 }, 'comercio_id': ?1 }")
    List<Promocion> findPromocionesActivasPorComercio(LocalDate hoy, String comercioId);
}

