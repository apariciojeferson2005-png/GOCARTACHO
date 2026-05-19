package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de acceso a datos para la entidad Auditoria en MongoDB.
 */
@Repository
public interface AuditoriaRepository extends MongoRepository<Auditoria, String> {
    Page<Auditoria> findAllByOrderByFechaDesc(Pageable pageable);
}

