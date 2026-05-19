package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.PuntoCalor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.geo.Distance;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad PuntoCalor en MongoDB.
 */
@Repository
public interface PuntoCalorRepository extends MongoRepository<PuntoCalor, String> {

    // Buscar el último punto registrado por un dispositivo específico para
    // optimizar espacio
    Optional<PuntoCalor> findByDispositivoHash(String dispositivoHash);

    // Traer puntos de calor posteriores a un instante de tiempo
    List<PuntoCalor> findByTimestampAfter(LocalDateTime timestamp);

    // Contar puntos activos cercanos a una ubicación (Usa el índice 2DSPHERE
    // internamente)
    long countByUbicacionNearAndTimestampAfter(GeoJsonPoint ubicacion, Distance maxDistance, LocalDateTime timestamp);
}
