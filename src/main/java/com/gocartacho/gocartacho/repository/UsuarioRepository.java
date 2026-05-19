package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repositorio de acceso a datos para la entidad Usuario en MongoDB.
 */
@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    // Para el login y para verificar si un email ya existe.
    Optional<Usuario> findByEmail(String email);

    // Para verificar si un nombre de usuario ya existe.
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);

    // Permite login por email o por nombre de usuario
    Optional<Usuario> findByEmailOrUsername(String email, String username);

    // Búsqueda para el panel de administración con soporte para paginación.
    Page<Usuario> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(
            String nombre, String apellido, String email, String username, Pageable pageable);
}
