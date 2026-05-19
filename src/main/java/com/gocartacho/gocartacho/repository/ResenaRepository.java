package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.Resena;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResenaRepository extends MongoRepository<Resena, String> {

    // Listar reseñas para un comercio específico, ordenadas por fecha reciente (sin
    // paginación)
    List<Resena> findByComercioIdOrderByFechaDesc(String comercioId);

    // Listar reseñas paginadas para un comercio específico
    Page<Resena> findByComercioId(String comercioId, Pageable pageable);

    // Listar reseñas reportadas
    List<Resena> findByReportadaTrueOrderByFechaDesc();

    // Contar reseñas de un comercio
    long countByComercioId(String comercioId);

    // Eliminar todas las reseñas asociadas a un usuario
    void deleteByUsuarioId(String usuarioId);

    // Verificar si un usuario ya reseñó un comercio específico
    boolean existsByUsuarioIdAndComercioId(String usuarioId, String comercioId);

    /**
     * Agrega la calificación promedio de un comercio.
     * La query de agregación calcula el promedio del campo 'calificacion' para un
     * comercioId dado.
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'comercio_id': ?0 } }",
            "{ '$group': { '_id': '$comercio_id', 'promedio': { '$avg': '$calificacion' } } }"
    })
    Double calcularPromedioPorComercioId(String comercioId);
}