package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.Favorito;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Favorito en MongoDB.
 */
@Repository
public interface FavoritoRepository extends MongoRepository<Favorito, String> {
    Optional<Favorito> findByUsuarioIdAndComercioId(String usuarioId, String comercioId);

    List<Favorito> findByUsuarioIdOrderByFechaAgregadoDesc(String usuarioId);

    // Eliminar todos los favoritos asociados a un usuario
    void deleteByUsuarioId(String usuarioId);

    // Contar favoritos por comercio
    long countByComercioId(String comercioId);
}
