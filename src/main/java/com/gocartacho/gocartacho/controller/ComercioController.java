package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.dto.ComercioDTO;
import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.EstadoComercio;
import com.gocartacho.gocartacho.mapper.ComercioMapper;
import com.gocartacho.gocartacho.service.ComercioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.core.Authentication;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.service.NotificacionService;
import com.gocartacho.gocartacho.service.AuditoriaService;
import com.gocartacho.gocartacho.service.UsuarioService;
import com.gocartacho.gocartacho.repository.FavoritoRepository;
import com.gocartacho.gocartacho.service.PromocionService;
import com.gocartacho.gocartacho.model.Promocion;
import com.gocartacho.gocartacho.repository.PuntoCalorRepository;
import java.util.Map;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/comercios")
@RequiredArgsConstructor
public class ComercioController {

    private final ComercioService comercioService;
    private final ComercioMapper comercioMapper;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final UsuarioService usuarioService;
    private final FavoritoRepository favoritoRepository;
    private final PromocionService promocionService;
    private final PuntoCalorRepository puntoCalorRepository;

    /**
     * Devuelve el primer comercio activo/pendiente asociado a un usuario.
     * Usado por mi-negocio.html para detectar si el usuario ya tiene negocio.
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<Comercio> obtenerComercioPorUsuario(@PathVariable String usuarioId) {
        List<Comercio> comercios = comercioService.obtenerComerciosPorPropietarioYEstados(
                usuarioId,
                List.of(EstadoComercio.APROBADO, EstadoComercio.PENDIENTE, EstadoComercio.INACTIVO));
        if (comercios == null || comercios.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(comercios.get(0));
    }

    @GetMapping("/zona/{zonaId}")
    public ResponseEntity<List<Comercio>> obtenerComerciosPorZona(
            @PathVariable String zonaId,
            @RequestParam(required = false) Long tipoId) {

        log.info("Consultando comercios para la zona ID: {} con filtro tipo: {}", zonaId, tipoId);
        List<Comercio> comercios = (tipoId != null)
                ? comercioService.obtenerComerciosPorZonaYTipo(zonaId, tipoId)
                : comercioService.obtenerComerciosPorZona(zonaId);

        return ResponseEntity.ok(comercios);
    }

    /**
     * Detalle público de un comercio.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Comercio> obtenerComercioPorId(@PathVariable String id) {
        Comercio comercio = comercioService.obtenerComercioPorId(id);
        return (comercio != null) ? ResponseEntity.ok(comercio) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<Page<Comercio>> obtenerTodos(Pageable pageable,
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(comercioService.obtenerTodosLosComercios(nombre, pageable));
    }

    /**
     * Registro de comercio (Cualquier usuario autenticado).
     * Se usa @Valid para activar las validaciones de coordenadas y contacto.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComercioDTO registrarComercio(@Valid @RequestBody ComercioDTO comercioDto, Authentication authentication) {
        log.info("Registrando nuevo comercio: {}", comercioDto.getNombre());

        Comercio comercio = comercioMapper.toEntity(comercioDto);
        if (authentication != null && authentication.getName() != null) {
            String email = authentication.getName();
            Usuario propietario = usuarioService.obtenerUsuarioPorEmail(email).orElse(null);
            if (propietario != null) {
                comercio.setPropietarioId(propietario.getUsuarioId());
            }
        }

        Comercio guardado = comercioService.guardarComercio(comercio);
        return comercioMapper.toDto(guardado);
    }

    /**
     * Actualización de datos. El dueño o un ADMIN pueden realizarla.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Object> actualizarComercio(@PathVariable String id,
            @Valid @RequestBody ComercioDTO comercioDto, Authentication authentication) {

        Comercio comercioExistente = comercioService.obtenerComercioPorId(id);
        if (comercioExistente == null) {
            return ResponseEntity.notFound().build();
        }

        String username = authentication.getName();
        Usuario propietario = comercioExistente.getPropietarioId() != null
                ? usuarioService.obtenerUsuarioPorId(comercioExistente.getPropietarioId()).orElse(null)
                : null;
        boolean isOwner = propietario != null && username.equals(propietario.getEmail());
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(403).body("No tienes permiso para actualizar este comercio.");
        }

        Comercio comercio = comercioMapper.toEntity(comercioDto);
        comercio.setComercioId(id);
        comercio.setPropietarioId(comercioExistente.getPropietarioId());
        Comercio actualizado = comercioService.guardarComercio(comercio);

        if (isAdmin) {
            auditoriaService.registrarAccion(username, "EDITAR_COMERCIO", "COMERCIO", id,
                    "Cambios en info básica por admin");
        }

        return ResponseEntity.ok(comercioMapper.toDto(actualizado));
    }

    /**
     * Gestión administrativa (Solo ADMIN).
     */
    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Comercio>> obtenerPendientes(Pageable pageable,
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(comercioService.obtenerComerciosPorEstado(EstadoComercio.PENDIENTE, nombre, pageable));
    }

    /**
     * Obtener comercios inactivos (Solo ADMIN).
     */
    @GetMapping("/inactivos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Comercio>> obtenerInactivos(Pageable pageable,
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(comercioService.obtenerComerciosPorEstado(EstadoComercio.INACTIVO, nombre, pageable));
    }

    /**
     * Cambio de estado de aprobación (Solo ADMIN).
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Comercio> actualizarEstado(
            @PathVariable String id,
            @RequestParam EstadoComercio estado,
            @RequestParam(required = false) String motivo,
            Authentication authentication) {

        Comercio comercio = comercioService.obtenerComercioPorId(id);

        if (comercio == null)
            return ResponseEntity.notFound().build();

        comercio.setEstadoAprobacion(estado);
        Comercio actualizado = comercioService.guardarComercio(comercio);

        // Desactivar promociones si el comercio se desactiva o rechaza
        if (EstadoComercio.INACTIVO == estado || EstadoComercio.RECHAZADO == estado) {
            try {
                List<Promocion> activePromos = promocionService.obtenerPromocionesActivasPorComercio(id);
                if (activePromos != null) {
                    for (Promocion p : activePromos) {
                        promocionService.desactivarPromocion(p.getPromocionId());
                    }
                }
            } catch (Exception ex) {
                log.error("Error al desactivar promociones asociadas al comercio deshabilitado: {}", id, ex);
            }
        }

        // Notificación
        if (comercio.getPropietarioId() != null) {
            Usuario propietario = usuarioService.obtenerUsuarioPorId(comercio.getPropietarioId()).orElse(null);
            if (propietario != null) {
                String titulo = "Actualización de tu negocio: " + comercio.getNombre();
                String mensaje = "El estado de tu negocio ha cambiado a: " + estado;
                if (EstadoComercio.RECHAZADO == estado || EstadoComercio.INACTIVO == estado) {
                    mensaje = "Tu negocio ha sido " + estado + (motivo != null ? ". Motivo: " + motivo : ".");
                }
                notificacionService.enviarNotificacion(propietario, titulo, mensaje);
            }
        }

        // Auditoría
        auditoriaService.registrarAccion(authentication.getName(), "CAMBIO_ESTADO_" + estado,
                "COMERCIO", id, "Motivo: " + (motivo != null ? motivo : "N/A"));

        log.info("Comercio ID {} cambiado a estado: {}. Por admin: {}", id, estado, authentication.getName());
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Eliminar comercio físicamente (Solo ADMIN).
     * Usado al rechazar un negocio en el panel.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarComercioRechazado(
            @PathVariable String id,
            @RequestParam(required = false) String motivo,
            Authentication authentication) {
        Comercio comercio = comercioService.obtenerComercioPorId(id);
        if (comercio == null)
            return ResponseEntity.notFound().build();

        // Notificación de rechazo y eliminación
        if (comercio.getPropietarioId() != null) {
            Usuario propietario = usuarioService.obtenerUsuarioPorId(comercio.getPropietarioId()).orElse(null);
            if (propietario != null) {
                String titulo = "Solicitud Rechazada: " + comercio.getNombre();
                String mensaje = "Lamentablemente tu solicitud para registrar el negocio ha sido rechazada"
                        + (motivo != null && !motivo.isBlank() ? " por el siguiente motivo: " + motivo
                                : " por no cumplir las directrices de la plataforma.");
                notificacionService.enviarNotificacion(propietario, titulo, mensaje);
            }
        }

        // Realizar borrado lógico (Soft Delete) cambiando su estado a RECHAZADO
        comercio.setEstadoAprobacion(EstadoComercio.RECHAZADO);
        comercioService.guardarComercio(comercio);

        // Desactivar promociones al ser rechazado
        try {
            List<Promocion> activePromos = promocionService.obtenerPromocionesActivasPorComercio(id);
            if (activePromos != null) {
                for (Promocion p : activePromos) {
                    promocionService.desactivarPromocion(p.getPromocionId());
                }
            }
        } catch (Exception ex) {
            log.error("Error al desactivar promociones asociadas al comercio rechazado: {}", id, ex);
        }

        // Auditoría
        String detalleAuditoria = "Negocio rechazado (Soft Delete)."
                + (motivo != null && !motivo.isBlank() ? " Motivo: " + motivo : " Sin motivo especificado.");
        auditoriaService.registrarAccion(authentication.getName(), "RECHAZAR_COMERCIO_LOGICO",
                "COMERCIO", id, detalleAuditoria);

        log.info("Comercio ID {} rechazado lógicamente (Soft Delete). Motivo: {}. Por admin: {}", id, motivo,
                authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene las estadísticas dinámicas y reales de un comercio.
     * Segurizado para que solo el propietario o un ADMIN puedan consultarlas.
     */
    @GetMapping("/{id}/estadisticas")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Object> obtenerEstadisticasComercio(@PathVariable String id, Authentication authentication) {
        Comercio comercio = comercioService.obtenerComercioPorId(id);
        if (comercio == null) {
            return ResponseEntity.notFound().build();
        }

        // Validar que el usuario autenticado sea el dueño o un ADMIN
        String username = authentication.getName();
        Usuario propietario = comercio.getPropietarioId() != null
                ? usuarioService.obtenerUsuarioPorId(comercio.getPropietarioId()).orElse(null)
                : null;
        boolean isOwner = propietario != null && username.equals(propietario.getEmail());
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("No tienes permiso para acceder a las estadísticas de este comercio.");
        }

        // Calcular datos reales
        long totalFavoritos = favoritoRepository.countByComercioId(id);
        long totalPromosActivas = promocionService.obtenerPromocionesActivasPorComercio(id).size();

        // Calcular visitantes activos reales alrededor del negocio (+/- 0.0015 grados,
        // aprox. 166 metros)
        long totalVisitantesActivos = 0;
        if (comercio.getLatitud() != null && comercio.getLongitud() != null) {
            double cLat = comercio.getLatitud().doubleValue();
            double cLng = comercio.getLongitud().doubleValue();
            totalVisitantesActivos = puntoCalorRepository.findAll().stream()
                    .filter(p -> {
                        if (p.getLatitud() == null || p.getLongitud() == null)
                            return false;
                        double pLat = p.getLatitud().doubleValue();
                        double pLng = p.getLongitud().doubleValue();
                        return Math.abs(pLat - cLat) <= 0.0015 && Math.abs(pLng - cLng) <= 0.0015;
                    })
                    .count();
        }

        return ResponseEntity.ok(Map.of(
                "totalFavoritos", totalFavoritos,
                "totalResenas", comercio.getTotalResenas() != null ? comercio.getTotalResenas() : 0,
                "promedioCalificacion",
                comercio.getPromedioCalificacion() != null ? comercio.getPromedioCalificacion() : 0.0,
                "totalPromocionesActivas", totalPromosActivas,
                "totalVisitantesActivos", totalVisitantesActivos));
    }

}