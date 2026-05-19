package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.model.Notificacion;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.service.NotificacionService;
import com.gocartacho.gocartacho.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para manejar las peticiones HTTP relacionadas con Notificacion.
 */
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<Notificacion>> obtenerMias(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerUsuarioPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        return ResponseEntity.ok(notificacionService.obtenerPorUsuario(usuario.getUsuarioId()));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<Void> marcarLeida(@PathVariable String id) {
        notificacionService.marcarComoLeida(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        notificacionService.eliminarNotificacion(id);
        return ResponseEntity.ok().build();
    }
}

