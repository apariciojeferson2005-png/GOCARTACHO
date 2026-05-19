package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.Notificacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad Notificacion en MongoDB.
 */
@Repository
public interface NotificacionRepository extends MongoRepository<Notificacion, String> {
    List<Notificacion> findByUsuarioIdOrderByFechaDesc(String usuarioId);

    List<Notificacion> findByUsuarioIdAndLeidaFalse(String usuarioId);
}
