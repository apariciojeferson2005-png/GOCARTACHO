package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.dto.ResenaRequest;
import com.gocartacho.gocartacho.model.Resena;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.service.ResenaService;
import com.gocartacho.gocartacho.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller de Reseñas.
 * Migrado a inyección por constructor (RequiredArgsConstructor), consistente con el resto.
 */
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ResenaController {

    private final ResenaService resenaService;
    private final UsuarioService usuarioService;
    private final com.gocartacho.gocartacho.service.AuditoriaService auditoriaService;
    private final com.gocartacho.gocartacho.service.NotificacionService notificacionService;

    // Rate limiting para reportes
    private final Cache<String, Bucket> reportBuckets = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private Bucket resolverBucket(String key) {
        return reportBuckets.get(key, k -> {
            // Permitir 3 reportes por hora por usuario/IP
            Bandwidth limit = Bandwidth.builder()
                    .capacity(3)
                    .refillGreedy(3, Duration.ofHours(1))
                    .build();
            return Bucket.builder().addLimit(limit).build();
        });
    }

    /**
     * Obtiene las reseñas de un comercio con paginación.
     * URL: GET /api/v1/comercios/{comercioId}/resenas?page=0&size=10
     */
    @GetMapping("/comercios/{comercioId}/resenas")
    public ResponseEntity<Page<Resena>> obtenerResenasDeComercio(
            @PathVariable String comercioId,
            @PageableDefault(size = 10, sort = "fecha") Pageable pageable) {
        Page<Resena> resenas = resenaService.obtenerResenasPorComercioPaginadas(comercioId, pageable);
        return ResponseEntity.ok(resenas);
    }

    /**
     * Obtiene el rating promedio de un comercio.
     * URL: GET /api/v1/comercios/{comercioId}/rating
     */
    @GetMapping("/comercios/{comercioId}/rating")
    public ResponseEntity<Map<String, Object>> obtenerRatingPromedio(@PathVariable String comercioId) {
        Double promedio = resenaService.calcularRatingPromedio(comercioId);
        return ResponseEntity.ok(Map.of(
                "comercioId", comercioId,
                "ratingPromedio", promedio != null ? promedio : 0.0,
                "tieneResenas", promedio != null
        ));
    }

    /**
     * Publica una nueva reseña.
     * URL: POST /api/v1/resenas
     *
     * SEGURIDAD: Requiere autenticación. El usuarioId se toma del contexto de seguridad,
     * NO del body del request, para impedir publicar reseñas en nombre de otro usuario.
     */
    @PostMapping("/resenas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> publicarResena(
            @Valid @RequestBody ResenaRequest request,
            Authentication authentication) {
        try {
            // Obtener el usuario autenticado desde el SecurityContext
            String emailAutenticado = authentication.getName();
            Usuario usuarioAutenticado = usuarioService.obtenerUsuarioPorEmail(emailAutenticado)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado."));

            // El usuarioId del body se ignora: se usa siempre el del usuario logueado
            Resena nuevaResena = resenaService.guardarResena(request, usuarioAutenticado.getUsuarioId());
            return ResponseEntity.ok(nuevaResena);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina una reseña perteneciente al usuario logueado.
     * URL: DELETE /api/v1/resenas/{resenaId}
     */
    @DeleteMapping("/resenas/{resenaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarResena(@PathVariable String resenaId, Authentication authentication) {
        try {
            String emailAutenticado = authentication.getName();
            Usuario usuarioAutenticado = usuarioService.obtenerUsuarioPorEmail(emailAutenticado)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado."));

            resenaService.eliminarResena(resenaId, usuarioAutenticado.getUsuarioId());
            return ResponseEntity.ok(Map.of("status", "success", "message", "Reseña eliminada correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Reporta una reseña por contenido inapropiado.
     */
    @PatchMapping("/resenas/{resenaId}/reportar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reportarResena(
            @PathVariable String resenaId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request,
            Authentication authentication) {
        String identifier = authentication.getName(); // Identificar por email del usuario
        Bucket bucket = resolverBucket(identifier);
        
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Has excedido el límite de reportes permitidos por hora."));
        }

        try {
            String emailAutenticado = authentication.getName();
            Usuario usuarioAutenticado = usuarioService.obtenerUsuarioPorEmail(emailAutenticado).orElse(null);
            String usuarioId = (usuarioAutenticado != null) ? usuarioAutenticado.getUsuarioId() : "ANÓNIMO";

            String motivo = "Ofensivo/Inapropiado";
            String detalles = "Reportado desde la aplicación móvil/web";
            if (body != null) {
                if (body.containsKey("motivo") && body.get("motivo") != null) {
                    motivo = body.get("motivo");
                }
                if (body.containsKey("detalles") && body.get("detalles") != null) {
                    detalles = body.get("detalles");
                }
            }

            resenaService.reportarResenaConMotivo(resenaId, usuarioId, motivo, detalles);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Reseña reportada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene la lista de reseñas reportadas (Solo ADMIN).
     */
    @GetMapping("/admin/resenas/reportadas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> obtenerResenasReportadas() {
        return ResponseEntity.ok(resenaService.obtenerResenasReportadas());
    }

    /**
     * Elimina una reseña por parte del administrador.
     */
    @DeleteMapping("/admin/resenas/{resenaId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminarResenaAdmin(
            @PathVariable String resenaId,
            @RequestParam(required = false) String motivo,
            Authentication authentication) {
        try {
            // Obtener la reseña y notificar al autor
            Resena resena = resenaService.obtenerResenaPorId(resenaId);
            if (resena != null && resena.getUsuarioId() != null) {
                Usuario autor = usuarioService.obtenerUsuarioPorId(resena.getUsuarioId()).orElse(null);
                if (autor != null) {
                    String titulo = "Reseña eliminada por moderación";
                    String mensaje = "Tu reseña ha sido eliminada por un moderador"
                            + (motivo != null && !motivo.isBlank() ? " por el siguiente motivo: " + motivo : " por infringir las normas de la comunidad.");
                    notificacionService.enviarNotificacion(autor, titulo, mensaje);
                }
            }

            resenaService.eliminarResenaAdmin(resenaId);

            String detalleAuditoria = "Reseña eliminada por admin." + (motivo != null && !motivo.isBlank() ? " Motivo: " + motivo : " Sin motivo especificado.");
            auditoriaService.registrarAccion(authentication.getName(), "BORRAR_RESENA", "RESENA", resenaId, detalleAuditoria);

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Descarta/Desestima un reporte de reseña (Solo ADMIN).
     */
    @PatchMapping("/admin/resenas/{resenaId}/descartar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> descartarReporteAdmin(
            @PathVariable String resenaId,
            @RequestParam(required = false) String motivo,
            Authentication authentication) {
        try {
            resenaService.descartarReporteResena(resenaId);

            String detalleAuditoria = "Reporte desestimado por admin." + (motivo != null && !motivo.isBlank() ? " Motivo: " + motivo : " Sin motivo especificado.");
            auditoriaService.registrarAccion(authentication.getName(), "DESCARTAR_REPORTE", "RESENA", resenaId, detalleAuditoria);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}