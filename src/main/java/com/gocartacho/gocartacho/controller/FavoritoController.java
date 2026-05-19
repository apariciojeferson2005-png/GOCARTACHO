package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.dto.ComercioDTO;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.service.FavoritoService;
import com.gocartacho.gocartacho.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/favoritos")
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService favoritoService;
    private final UsuarioService usuarioService;

    /**
     * Alterna (Toggle) el estado de favorito de un comercio para el usuario logueado.
     * Si no era favorito lo agrega, si ya era lo remueve.
     */
    @PostMapping("/{comercioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> toggleFavorito(@PathVariable String comercioId, Authentication authentication) {
        try {
            Usuario usuario = obtenerUsuarioAutenticado(authentication);
            String mensaje = favoritoService.toggleFavorito(usuario.getUsuarioId(), comercioId);
            return ResponseEntity.ok(Map.of("status", "success", "message", mensaje));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Lista todos los comercios marcados como favoritos por el usuario.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ComercioDTO>> obtenerMisFavoritos(Authentication authentication) {
        try {
            Usuario usuario = obtenerUsuarioAutenticado(authentication);
            List<ComercioDTO> favoritos = favoritoService.obtenerFavoritosUsuario(usuario.getUsuarioId());
            return ResponseEntity.ok(favoritos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private Usuario obtenerUsuarioAutenticado(Authentication authentication) {
        String email = authentication.getName();
        return usuarioService.obtenerUsuarioPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }
}
