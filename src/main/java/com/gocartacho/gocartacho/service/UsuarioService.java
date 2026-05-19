package com.gocartacho.gocartacho.service;

import com.gocartacho.gocartacho.model.Usuario;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    Usuario registrarUsuario(Usuario usuario) throws IllegalArgumentException;

    Usuario registrarAdministrador(Usuario usuario) throws IllegalArgumentException;

    Usuario registrarComerciante(Usuario usuario) throws IllegalArgumentException;

    Optional<Usuario> obtenerUsuarioPorEmail(String email);

    Usuario autenticarUsuario(String identificador, String contrasena) throws IllegalArgumentException;

    /** Retorna el número total de usuarios registrados. */
    long contarUsuarios();

    // Métodos CRUD para administradores
    Page<Usuario> obtenerTodosLosUsuarios(String terminoBusqueda, Pageable pageable);

    Optional<Usuario> obtenerUsuarioPorId(String id);

    /** Actualización completa de un usuario (solo ADMIN). Permite cambiar rol. */
    Usuario actualizarUsuario(Usuario usuario) throws IllegalArgumentException;

    void eliminarUsuario(String id) throws IllegalArgumentException;

    /**
     * Actualización del perfil propio del usuario logueado.
     * Permite cambiar nombre, apellido, username, contraseña, foto de perfil, biografía y tipo de viajero.
     * El rol no puede modificarse.
     */
    Usuario actualizarPerfilPropio(String usuarioId, String nuevoNombre, String nuevoApellido, String nuevoUsername,
            String nuevaContrasena, String nuevoFotoUrl, String nuevoBiografia, String nuevoTipoViajero)
            throws IllegalArgumentException;

    /** Permite al usuario logueado eliminar su propia cuenta de la plataforma. */
    void eliminarCuentaPropia(String usuarioId) throws IllegalArgumentException;

    /** Genera un token y envía un correo para recuperar la contraseña. */
    void solicitarRecuperacionContrasena(String email);

    /** Valida el token y establece una nueva contraseña para el usuario. */
    void restablecerContrasena(String token, String nuevaContrasena) throws IllegalArgumentException;

    /** Cambia la contraseña verificando la anterior. */
    void actualizarPassword(String usuarioId, String oldPassword, String newPassword) throws IllegalArgumentException;
}