package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.dto.LoginPeticion;
import com.gocartacho.gocartacho.dto.UsuarioResponseDTO;
import com.gocartacho.gocartacho.dto.PerfilUpdateDTO;
import com.gocartacho.gocartacho.dto.UsuarioRegistroDTO;
import com.gocartacho.gocartacho.dto.UsuarioUpdateDTO;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.repository.UsuarioRepository;
import com.gocartacho.gocartacho.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import com.gocartacho.gocartacho.security.UtilidadJwt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Controlador para manejar las peticiones HTTP relacionadas con Usuario.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final UtilidadJwt jwtUtil;
    private final com.gocartacho.gocartacho.service.AuditoriaService auditoriaService;
    private final com.gocartacho.gocartacho.service.EmailService emailService;

    public UsuarioController(
            UsuarioService usuarioService,
            UsuarioRepository usuarioRepository,
            AuthenticationManager authenticationManager,
            UtilidadJwt jwtUtil,
            com.gocartacho.gocartacho.service.AuditoriaService auditoriaService,
            com.gocartacho.gocartacho.service.EmailService emailService) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.auditoriaService = auditoriaService;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> registrarUsuario(@Valid @RequestBody UsuarioRegistroDTO dto) {
        try {
            Usuario usuarioMapeado = new Usuario();
            usuarioMapeado.setNombre(dto.getNombre());
            usuarioMapeado.setApellido(dto.getApellido());
            usuarioMapeado.setUsername(dto.getUsername());
            usuarioMapeado.setEmail(dto.getEmail());
            usuarioMapeado.setContrasena(dto.getContrasena());

            Usuario usuarioRegistrado = usuarioService.registrarUsuario(usuarioMapeado);
            return ResponseEntity.ok(new UsuarioResponseDTO(usuarioRegistrado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> registrarAdministrador(@Valid @RequestBody UsuarioRegistroDTO dto, Authentication authentication) {
        try {
            Usuario usuarioAdmin = new Usuario();
            usuarioAdmin.setNombre(dto.getNombre());
            usuarioAdmin.setApellido(dto.getApellido());
            usuarioAdmin.setUsername(dto.getUsername());
            usuarioAdmin.setEmail(dto.getEmail());
            usuarioAdmin.setContrasena(dto.getContrasena());

            Usuario usuarioRegistrado = usuarioService.registrarAdministrador(usuarioAdmin);
            
            auditoriaService.registrarAccion(authentication.getName(), "CREAR_ADMIN", "USUARIO", 
                    usuarioRegistrado.getUsuarioId().toString(), "Nuevo administrador: " + dto.getEmail());
                    
            return ResponseEntity.ok(new UsuarioResponseDTO(usuarioRegistrado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Object> loginUsuario(@RequestBody LoginPeticion loginRequest, HttpServletResponse response, jakarta.servlet.http.HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getIdentificador(), loginRequest.getContrasena())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = jwtUtil.generateToken(authentication.getName());

            org.springframework.http.ResponseCookie jwtCookie = org.springframework.http.ResponseCookie.from("jwt", java.util.Objects.requireNonNull(token))
                    .httpOnly(true)
                    .secure(request.isSecure())
                    .path("/")
                    .maxAge(24 * 60 * 60) // 1 día
                    .sameSite("Strict")
                    .build();
            response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, jwtCookie.toString());

            // Buscar al usuario por el identificador (puede ser email o username)
            Usuario usuario = usuarioRepository.findByEmailOrUsername(loginRequest.getIdentificador(), loginRequest.getIdentificador())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no en Base de datos"));

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "usuario", new UsuarioResponseDTO(usuario)
            ));
        } catch (org.springframework.security.core.AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario o contraseña incorrectos.");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response, jakarta.servlet.http.HttpServletRequest request) {
        // Invalidar la cookie JWT en el cliente
        org.springframework.http.ResponseCookie expiredCookie = org.springframework.http.ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, expiredCookie.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> refreshToken(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String newToken = jwtUtil.generateToken(email);
        // Renovar también la cookie httpOnly
        org.springframework.http.ResponseCookie jwtCookie = org.springframework.http.ResponseCookie.from("jwt", java.util.Objects.requireNonNull(newToken))
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict")
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, jwtCookie.toString());
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        usuarioService.solicitarRecuperacionContrasena(email);
        // Siempre retornamos OK para evitar ataques de enumeración de correos
        return ResponseEntity.ok(Map.of("message", "Si el correo existe en nuestro sistema, recibirás un enlace de recuperación."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> restablecerContrasena(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String nuevaContrasena = body.get("nuevaContrasena");
        try {
            usuarioService.restablecerContrasena(token, nuevaContrasena);
            return ResponseEntity.ok(Map.of("message", "Contraseña restablecida con éxito."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UsuarioResponseDTO> obtenerPerfilPropio() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.obtenerUsuarioPorEmail(email)
                .map(usuario -> ResponseEntity.ok(new UsuarioResponseDTO(usuario)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UsuarioResponseDTO> actualizarPerfilPropio(@Valid @RequestBody PerfilUpdateDTO dto) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario actual = usuarioService.obtenerUsuarioPorEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

            Usuario actualizado = usuarioService.actualizarPerfilPropio(
                    actual.getUsuarioId(), dto.getNombre(), dto.getApellido(), dto.getUsername(), dto.getContrasena(), dto.getFotoUrl(), dto.getBiografia(), dto.getTipoViajero());
            
            return ResponseEntity.ok(new UsuarioResponseDTO(actualizado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/usuarios/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UsuarioResponseDTO> updatePerfil(@RequestBody Map<String, String> updates) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario actual = usuarioService.obtenerUsuarioPorEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            Usuario actualizado = usuarioService.actualizarPerfilPropio(
                    actual.getUsuarioId(), updates.get("nombre"), updates.get("apellido"), null, null, null, null, null);
            return ResponseEntity.ok(new UsuarioResponseDTO(actualizado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/usuarios/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> updatePassword(@RequestBody Map<String, String> body) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario actual = usuarioService.obtenerUsuarioPorEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            usuarioService.actualizarPassword(actual.getUsuarioId(), body.get("oldPassword"), body.get("newPassword"));
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> eliminarCuentaPropia(Authentication authentication, HttpServletResponse response, jakarta.servlet.http.HttpServletRequest request) {
        try {
            String email = authentication.getName();
            Usuario actual = usuarioService.obtenerUsuarioPorEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

            usuarioService.eliminarCuentaPropia(actual.getUsuarioId());
            auditoriaService.registrarAccion(email, "ELIMINAR_CUENTA_PROPIA", "USUARIO", actual.getUsuarioId(), "El usuario eliminó su propia cuenta");
            
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerTodosLosUsuarios(
            @RequestParam(required = false) String nombre,
            @org.springframework.data.web.PageableDefault(size = 1000) org.springframework.data.domain.Pageable pageable) {
        List<UsuarioResponseDTO> usuarios = usuarioService.obtenerTodosLosUsuarios(nombre, pageable).getContent().stream()
                .map(UsuarioResponseDTO::new)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuarioPorId(@PathVariable String id) {
        return usuarioService.obtenerUsuarioPorId(id)
                .map(usuario -> ResponseEntity.ok(new UsuarioResponseDTO(usuario)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> actualizarUsuario(@PathVariable String id, @RequestBody UsuarioUpdateDTO usuarioDto) {
        try {
            Usuario usuario = new Usuario();
            usuario.setUsuarioId(id);
            usuario.setNombre(usuarioDto.getNombre());
            usuario.setApellido(usuarioDto.getApellido());
            usuario.setUsername(usuarioDto.getUsername());
            usuario.setEmail(usuarioDto.getEmail());
            // Map the String from DTO to Enum
            usuario.setRol(com.gocartacho.gocartacho.model.RolUsuario.valueOf(usuarioDto.getRol().toUpperCase()));
            
            if (usuarioDto.getEstado() != null && !usuarioDto.getEstado().isBlank()) {
                usuario.setEstado(com.gocartacho.gocartacho.model.EstadoUsuario.valueOf(usuarioDto.getEstado().toUpperCase()));
            }

            Usuario actualizado = usuarioService.actualizarUsuario(usuario);
            return ResponseEntity.ok(new UsuarioResponseDTO(actualizado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarUsuario(
            @PathVariable String id,
            @RequestParam(required = false) String motivo,
            Authentication authentication) {
        try {
            // Buscar el usuario para obtener su email y nombre antes de borrarlo
            Usuario usuario = usuarioService.obtenerUsuarioPorId(id).orElse(null);
            if (usuario != null && usuario.getEmail() != null) {
                emailService.enviarCorreoEliminacionCuenta(
                        usuario.getEmail(),
                        usuario.getNombre() != null ? usuario.getNombre() : usuario.getUsername(),
                        motivo
                );
            }

            usuarioService.eliminarUsuario(id);

            String detalleAuditoria = "Usuario eliminado por admin." + (motivo != null && !motivo.isBlank() ? " Motivo: " + motivo : " Sin motivo especificado.");
            auditoriaService.registrarAccion(authentication.getName(), "ELIMINAR_USUARIO", "USUARIO", id, detalleAuditoria);

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
