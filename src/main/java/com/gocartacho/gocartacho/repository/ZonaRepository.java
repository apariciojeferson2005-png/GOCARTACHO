package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.Zona;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Zona en MongoDB.
 */
@Repository
public interface ZonaRepository extends MongoRepository<Zona, String> {

    /** Busca una zona por su número entero fijo (1, 2, 3...) en lugar de por ObjectId. */
    Optional<Zona> findByNumero(Integer numero);

}
