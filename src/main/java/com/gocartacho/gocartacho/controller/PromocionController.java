package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.EstadoComercio;
import com.gocartacho.gocartacho.model.Promocion;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.dto.PromocionDTO;
import com.gocartacho.gocartacho.mapper.PromocionMapper;
import com.gocartacho.gocartacho.service.ComercioService;
import com.gocartacho.gocartacho.service.PromocionService;
import com.gocartacho.gocartacho.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import java.util.List;

/**
 * Controlador para manejar las peticiones HTTP relacionadas con Promocion.
 */
@RestController
@RequestMapping("/api/v1/promociones")
@RequiredArgsConstructor
public class PromocionController {

    private final PromocionService promocionService;
    private final ComercioService comercioService;
    private final PromocionMapper promocionMapper;
    private final UsuarioService usuarioService;

    @GetMapping("/activas")
    public ResponseEntity<List<PromocionDTO>> obtenerPromocionesActivas() {
        return ResponseEntity.ok(promocionMapper.toDtoList(promocionService.obtenerPromocionesActivas()));
    }

    @GetMapping("/zona/{zonaId}")
    public ResponseEntity<List<PromocionDTO>> obtenerPromocionesActivasPorZona(@PathVariable String zonaId) {
        return ResponseEntity.ok(promocionMapper.toDtoList(promocionService.obtenerPromocionesActivasPorZona(zonaId)));
    }

    @GetMapping("/comercio/{comercioId}")
    public ResponseEntity<List<PromocionDTO>> obtenerPromocionesDeComercio(@PathVariable String comercioId) {
        return ResponseEntity.ok(promocionMapper.toDtoList(promocionService.obtenerPromocionesActivasPorComercio(comercioId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Object> crearPromocion(@RequestBody PromocionDTO promocionDto, @RequestParam String comercioId,
                                                 Authentication authentication) {
        try {
            Comercio comercio = comercioService.obtenerComercioPorId(comercioId);
            if (comercio == null) {
                return ResponseEntity.badRequest().body("Comercio no encontrado.");
            }

            String username = authentication.getName();
            Usuario propietario = comercio.getPropietarioId() != null 
                    ? usuarioService.obtenerUsuarioPorId(comercio.getPropietarioId()).orElse(null) 
                    : null;
            boolean isOwner = propietario != null && username.equals(propietario.getEmail());
            boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isOwner && !isAdmin) {
                return ResponseEntity.status(403).body("No tienes permiso para crear promociones para este comercio.");
            }
            if (comercio.getEstadoAprobacion() != EstadoComercio.APROBADO) {
                return ResponseEntity.badRequest().body("Solo se pueden crear promociones para comercios aprobados.");
            }

            Promocion nuevaEntidad = promocionService.crearPromocion(promocionMapper.toEntity(promocionDto), comercioId);
            return ResponseEntity.ok(promocionMapper.toDto(nuevaEntidad));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Object> desactivarPromocion(@PathVariable String id, Authentication authentication) {
        try {
            Promocion promocion = promocionService.buscarEntidadPorId(id);
            Comercio comercio = comercioService.obtenerComercioPorId(promocion.getComercioId());

            String username = authentication.getName();
            Usuario propietario = comercio.getPropietarioId() != null 
                    ? usuarioService.obtenerUsuarioPorId(comercio.getPropietarioId()).orElse(null) 
                    : null;
            boolean isOwner = propietario != null && username.equals(propietario.getEmail());
            boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isOwner && !isAdmin) {
                return ResponseEntity.status(403).body("No tienes permiso para desactivar esta promoción.");
            }

            promocionService.desactivarPromocion(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromocionDTO> obtenerPromocion(@PathVariable String id) {
        PromocionDTO dto = promocionService.buscarPorId(id);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Object> cambiarEstadoPromocion(@PathVariable String id, @RequestParam boolean activa, Authentication authentication) {
        try {
            Promocion promocion = promocionService.buscarEntidadPorId(id);
            Comercio comercio = comercioService.obtenerComercioPorId(promocion.getComercioId());

            String username = authentication.getName();
            Usuario propietario = comercio.getPropietarioId() != null 
                    ? usuarioService.obtenerUsuarioPorId(comercio.getPropietarioId()).orElse(null) 
                    : null;
            boolean isOwner = propietario != null && username.equals(propietario.getEmail());
            boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isOwner && !isAdmin) {
                return ResponseEntity.status(403).body("No tienes permiso para cambiar el estado de esta promoción.");
            }

            promocionService.cambiarEstado(id, activa);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

