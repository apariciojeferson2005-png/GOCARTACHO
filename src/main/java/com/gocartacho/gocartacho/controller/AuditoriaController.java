package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.model.Auditoria;
import com.gocartacho.gocartacho.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador para manejar las peticiones HTTP relacionadas con Auditoria.
 */
@RestController
@RequestMapping("/api/v1/admin/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    public ResponseEntity<Page<Auditoria>> obtenerLogs(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime desde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime hasta,
            @RequestParam(required = false) String adminEmail,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String entidadId,
            Pageable pageable) {

        if (desde == null && hasta == null && 
                (adminEmail == null || adminEmail.isBlank()) &&
                (accion == null || accion.isBlank()) &&
                (entidad == null || entidad.isBlank()) &&
                (entidadId == null || entidadId.isBlank())) {
            return ResponseEntity.ok(auditoriaService.obtenerLogs(pageable));
        }

        return ResponseEntity.ok(auditoriaService.obtenerLogsFiltrados(
                desde, hasta, adminEmail, accion, entidad, entidadId, pageable));
    }
}
