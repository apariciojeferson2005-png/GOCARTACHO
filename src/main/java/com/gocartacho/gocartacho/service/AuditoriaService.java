package com.gocartacho.gocartacho.service;

import com.gocartacho.gocartacho.model.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define las operaciones de negocio para Auditoria.
 */
public interface AuditoriaService {
    void registrarAccion(String adminEmail, String accion, String entidad, String entidadId, String detalles);
    Page<Auditoria> obtenerLogs(Pageable pageable);
    Page<Auditoria> obtenerLogsFiltrados(
            java.time.LocalDateTime desde,
            java.time.LocalDateTime hasta,
            String adminEmail,
            String accion,
            String entidad,
            String entidadId,
            Pageable pageable);
}

