package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.RolUsuario;
import com.gocartacho.gocartacho.model.EstadoUsuario;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.repository.UsuarioRepository;
import com.gocartacho.gocartacho.repository.FavoritoRepository;
import com.gocartacho.gocartacho.repository.ResenaRepository;
import com.gocartacho.gocartacho.repository.TokenRecuperacionRepository;
import com.gocartacho.gocartacho.model.TokenRecuperacion;
import com.gocartacho.gocartacho.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final FavoritoRepository favoritoRepository;
    private final ResenaRepository resenaRepository;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final com.gocartacho.gocartacho.service.EmailService emailService;
    private final com.gocartacho.gocartacho.repository.ComercioRepository comercioRepository;
    private final com.gocartacho.gocartacho.service.AuditoriaService auditoriaService;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    // Al menos 8 caracteres, al menos una letra y un número
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

    private boolean esEmailValido(String email) {
        if (email == null)
            return false;
        return java.util.regex.Pattern.compile(EMAIL_REGEX).matcher(email).matches();
    }

    private boolean esContrasenaValida(String contrasena) {
        if (contrasena == null)
            return false;
        return java.util.regex.Pattern.compile(PASSWORD_REGEX).matcher(contrasena).matches();
    }

    private void validarDatosBasicos(Usuario usuario) {
        if (usuario.getEmail() == null || usuario.getContrasena() == null || usuario.getUsername() == null) {
            throw new IllegalArgumentException("El email, la contraseña y el nombre de usuario son obligatorios.");
        }
        if (!esEmailValido(usuario.getEmail())) {
            throw new IllegalArgumentException("El formato del correo electrónico es inválido.");
        }
        if (!esContrasenaValida(usuario.getContrasena())) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres, incluyendo una letra y un número.");
        }
    }

    @Override
    public Usuario registrarUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo.");
        }
        validarDatosBasicos(usuario);

        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email '" + usuario.getEmail() + "' ya está registrado.");
        }

        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario '" + usuario.getUsername() + "' ya está en uso.");
        }

        try {
            usuario.setRol(RolUsuario.USER);
            usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
            return usuarioRepository.save(usuario);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new IllegalArgumentException(
                    "El email o nombre de usuario ya fueron registrados simultáneamente.");
        }
    }

    @Override
    public Usuario registrarAdministrador(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo.");
        }
        validarDatosBasicos(usuario);
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email '" + usuario.getEmail() + "' ya está registrado.");
        }
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario '" + usuario.getUsername() + "' ya está en uso.");
        }

        usuario.setRol(RolUsuario.ADMIN);
        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario registrarComerciante(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo.");
        }
        validarDatosBasicos(usuario);
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email '" + usuario.getEmail() + "' ya está registrado.");
        }
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario '" + usuario.getUsername() + "' ya está en uso.");
        }

        // Asigna explícitamente el nuevo rol
        usuario.setRol(RolUsuario.COMERCIANTE);
        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        return usuarioRepository.save(usuario);
    }

    @Override
    public Optional<Usuario> obtenerUsuarioPorEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        return usuarioRepository.findByEmail(email);
    }

    @Override
    public long contarUsuarios() {
        return usuarioRepository.count();
    }

    @Override
    public Page<Usuario> obtenerTodosLosUsuarios(String terminoBusqueda, Pageable pageable) {
        Pageable p = (pageable != null) ? pageable : Pageable.unpaged();
        if (terminoBusqueda == null || terminoBusqueda.isBlank()) {
            return usuarioRepository.findAll(Objects.requireNonNull(p));
        }
        return usuarioRepository
                .findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(
                        terminoBusqueda, terminoBusqueda, terminoBusqueda, terminoBusqueda, Objects.requireNonNull(p));
    }

    @Override
    public Optional<Usuario> obtenerUsuarioPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return usuarioRepository.findById(Objects.requireNonNull(id));
    }

    @Override
    public Usuario actualizarUsuario(Usuario usuario) {
        if (usuario == null || usuario.getUsuarioId() == null) {
            throw new IllegalArgumentException("Datos de usuario inválidos para actualizar.");
        }

        Usuario existente = usuarioRepository.findById(Objects.requireNonNull(usuario.getUsuarioId()))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        // Validar email
        if (usuario.getEmail() != null && !usuario.getEmail().equals(existente.getEmail())) {
            if (!esEmailValido(usuario.getEmail())) {
                throw new IllegalArgumentException("El formato del correo electrónico es inválido.");
            }
            existente.setEmail(usuario.getEmail());
        }

        // Actualizar solo campos no nulos
        if (usuario.getNombre() != null) {
            existente.setNombre(usuario.getNombre());
        }
        if (usuario.getApellido() != null) {
            existente.setApellido(usuario.getApellido());
        }
        if (usuario.getUsername() != null) {
            // Verificar si el nuevo username está disponible
            if (!usuario.getUsername().equals(existente.getUsername()) &&
                    usuarioRepository.existsByUsername(usuario.getUsername())) {
                throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
            }
            existente.setUsername(usuario.getUsername());
        }
        if (usuario.getRol() != null) {
            existente.setRol(usuario.getRol());
        }
        if (usuario.getEstado() != null) {
            if (existente.getRol() == RolUsuario.SUPER_ADMIN && usuario.getEstado() != EstadoUsuario.ACTIVO) {
                throw new IllegalStateException("No se puede suspender o banear al Super Administrador maestro.");
            }
            existente.setEstado(usuario.getEstado());
        }

        // NOTA: La contraseña NO se actualiza aquí (se usa actualizarPerfilPropio para
        // eso)
        // Esto evita que el admin sobreescriba la contraseña con null accidentalmente.

        try {
            return usuarioRepository.save(Objects.requireNonNull(existente));
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new IllegalArgumentException(
                    "El email '" + usuario.getEmail() + "' ya está en uso por otro usuario.");
        }
    }

    @Override
    public void eliminarUsuario(String id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo.");
        }
        Usuario target = usuarioRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        // Bloquear cualquier intento de borrar al SUPER_ADMIN maestro
        if (target.getRol() == RolUsuario.SUPER_ADMIN) {
            throw new IllegalStateException("No se puede eliminar a un Super Administrador de la plataforma.");
        }

        // Obtener el usuario autenticado desde el contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String emailOrUsername = auth.getName();
            Optional<Usuario> actorOpt = usuarioRepository.findByEmailOrUsername(emailOrUsername, emailOrUsername);
            if (actorOpt.isPresent()) {
                Usuario actor = actorOpt.get();
                // Si el actor es un ADMIN regular (no SUPER_ADMIN)
                if (actor.getRol() == RolUsuario.ADMIN) {
                    if (target.getRol() == RolUsuario.ADMIN || target.getRol() == RolUsuario.SUPER_ADMIN) {
                        throw new IllegalStateException("Un administrador regular no tiene permisos para eliminar a otro administrador.");
                    }
                }
            }
        }

        // Eliminación en cascada
        favoritoRepository.deleteByUsuarioId(id);
        resenaRepository.deleteByUsuarioId(id);
        comercioRepository.deleteByPropietarioId(id);
        usuarioRepository.deleteById(Objects.requireNonNull(id));

        auditoriaService.registrarAccion(
                (auth != null) ? auth.getName() : "SISTEMA", 
                "BORRAR_USUARIO", 
                "USUARIO", 
                id,
                "Cuenta eliminada por un administrador"
        );
    }

    @Override
    public Usuario autenticarUsuario(String identificador, String contrasena) {
        if (identificador == null || identificador.trim().isEmpty() || contrasena == null || contrasena.isEmpty()) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
        }

        Usuario usuario = usuarioRepository.findByEmailOrUsername(identificador, identificador)
                .orElseThrow(() -> new IllegalArgumentException("Usuario o contraseña incorrectos."));

        if (!passwordEncoder.matches(contrasena, usuario.getContrasena())) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
        }

        return usuario;
    }

    /**
     * Permite al propio usuario actualizar su nombre y/o contraseña.
     * El rol no se puede modificar a través de este método (solo ADMIN puede
     * cambiar roles).
     */
    @Override
    public Usuario actualizarPerfilPropio(String usuarioId, String nuevoNombre, String nuevoApellido,
            String nuevoUsername, String nuevaContrasena, String nuevoFotoUrl, String nuevoBiografia, String nuevoTipoViajero) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("El ID de usuario no puede ser nulo.");
        }

        Usuario user = usuarioRepository.findById(Objects.requireNonNull(usuarioId))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (nuevoNombre != null && !nuevoNombre.isBlank()) {
            user.setNombre(nuevoNombre);
        }
        if (nuevoApellido != null && !nuevoApellido.isBlank()) {
            user.setApellido(nuevoApellido);
        }
        if (nuevoUsername != null && !nuevoUsername.isBlank()) {
            if (!nuevoUsername.equals(user.getUsername()) && usuarioRepository.existsByUsername(nuevoUsername)) {
                throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
            }
            user.setUsername(nuevoUsername);
        }
        if (nuevoFotoUrl != null) {
            user.setFotoUrl(nuevoFotoUrl.isBlank() ? null : nuevoFotoUrl);
        }
        if (nuevoBiografia != null) {
            user.setBiografia(nuevoBiografia.isBlank() ? null : nuevoBiografia);
        }
        if (nuevoTipoViajero != null) {
            user.setTipoViajero(nuevoTipoViajero.isBlank() ? null : nuevoTipoViajero);
        }
        if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
            if (!esContrasenaValida(nuevaContrasena)) {
                throw new IllegalArgumentException(
                        "La contraseña debe tener al menos 8 caracteres, incluyendo una letra y un número.");
            }
            user.setContrasena(passwordEncoder.encode(nuevaContrasena));
        }
        return usuarioRepository.save(Objects.requireNonNull(user));
    }

    @Override
    public void eliminarCuentaPropia(String usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("El ID de usuario no puede ser nulo.");
        }
        if (!usuarioRepository.existsById(Objects.requireNonNull(usuarioId))) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }

        // Eliminación en cascada de dependencias
        favoritoRepository.deleteByUsuarioId(usuarioId);
        resenaRepository.deleteByUsuarioId(usuarioId);
        comercioRepository.deleteByPropietarioId(usuarioId);

        // Eliminación del usuario principal
        usuarioRepository.deleteById(Objects.requireNonNull(usuarioId));

        auditoriaService.registrarAccion(usuarioId, "ELIMINAR_CUENTA_PROPIA", "USUARIO", usuarioId,
                "El usuario ha eliminado su propia cuenta de forma permanente");
    }

    @Override
    public void solicitarRecuperacionContrasena(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) {
            // Por seguridad, no revelamos si el correo existe o no en el sistema.
            return;
        }

        tokenRecuperacionRepository.deleteByEmailUsuario(email); // Limpiar tokens anteriores

        String token = java.util.UUID.randomUUID().toString();
        TokenRecuperacion tr = new TokenRecuperacion();
        tr.setToken(token);
        tr.setEmailUsuario(email);
        tr.setFechaExpiracion(LocalDateTime.now().plusMinutes(15));
        tokenRecuperacionRepository.save(tr);

        emailService.enviarCorreoRecuperacion(email, token);
    }

    @Override
    public void restablecerContrasena(String token, String nuevaContrasena) {
        if (!esContrasenaValida(nuevaContrasena)) {
            throw new IllegalArgumentException(
                    "La nueva contraseña debe tener al menos 8 caracteres, incluyendo una letra y un número.");
        }
        TokenRecuperacion tr = tokenRecuperacionRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El enlace de recuperación es inválido o ya fue utilizado."));

        if (tr.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            tokenRecuperacionRepository.delete(tr);
            throw new IllegalArgumentException("El enlace ha expirado. Por favor, solicita uno nuevo.");
        }

        Usuario usuario = usuarioRepository.findByEmail(tr.getEmailUsuario())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(java.util.Objects.requireNonNull(usuario));

        tokenRecuperacionRepository.delete(tr);

        auditoriaService.registrarAccion(usuario.getEmail(), "RESTABLECER_CONTRASENA", "USUARIO",
                usuario.getUsuarioId(),
                "Contraseña restablecida exitosamente mediante token de correo");
    }

    @Override
    public void actualizarPassword(String usuarioId, String oldPassword, String newPassword) {
        if (!esContrasenaValida(newPassword)) {
            throw new IllegalArgumentException(
                    "La nueva contraseña debe tener al menos 8 caracteres, incluyendo una letra y un número.");
        }
        Usuario user = usuarioRepository.findById(java.util.Objects.requireNonNull(usuarioId))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (!passwordEncoder.matches(oldPassword, user.getContrasena())) {
            throw new IllegalArgumentException("Contraseña actual incorrecta.");
        }

        user.setContrasena(passwordEncoder.encode(newPassword));
        usuarioRepository.save(java.util.Objects.requireNonNull(user));

        auditoriaService.registrarAccion(user.getEmail(), "ACTUALIZAR_CONTRASENA", "USUARIO", user.getUsuarioId(),
                "El usuario ha cambiado su contraseña desde su perfil");
    }
}