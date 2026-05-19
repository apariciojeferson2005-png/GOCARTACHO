package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.Auditoria;
import com.gocartacho.gocartacho.repository.AuditoriaRepository;
import com.gocartacho.gocartacho.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación de la lógica de negocio para Auditoria.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    @Transactional
    public void registrarAccion(String adminEmail, String accion, String entidad, String entidadId, String detalles) {
        Auditoria log = new Auditoria();
        log.setAdminEmail(adminEmail);
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setEntidadId(entidadId);
        log.setDetalles(detalles);
        log.setFecha(LocalDateTime.now());
        auditoriaRepository.save(log);
    }

    @Override
    public Page<Auditoria> obtenerLogs(Pageable pageable) {
        return auditoriaRepository.findAllByOrderByFechaDesc(pageable);
    }

    @Override
    @SuppressWarnings("null")
    public Page<Auditoria> obtenerLogsFiltrados(
            LocalDateTime desde,
            LocalDateTime hasta,
            String adminEmail,
            String accion,
            String entidad,
            String entidadId,
            Pageable pageable) {

        Query query = new Query();

        // 1. Filtro de Rango de Fechas
        if (desde != null && hasta != null) {
            query.addCriteria(Criteria.where("fecha").gte(desde).lte(hasta));
        } else if (desde != null) {
            query.addCriteria(Criteria.where("fecha").gte(desde));
        } else if (hasta != null) {
            query.addCriteria(Criteria.where("fecha").lte(hasta));
        }

        // 2. Filtros por Texto / Regex Parcial (Insensitive)
        if (adminEmail != null && !adminEmail.isBlank()) {
            query.addCriteria(Criteria.where("adminEmail").regex(adminEmail.trim(), "i"));
        }
        if (accion != null && !accion.isBlank()) {
            query.addCriteria(Criteria.where("accion").regex(accion.trim(), "i"));
        }
        if (entidad != null && !entidad.isBlank()) {
            query.addCriteria(Criteria.where("entidad").regex(entidad.trim(), "i"));
        }
        if (entidadId != null && !entidadId.isBlank()) {
            query.addCriteria(Criteria.where("entidadId").is(entidadId.trim()));
        }

        // 3. Obtener conteo total para paginación
        long total = mongoTemplate.count(query, Auditoria.class);

        // 4. Agregar orden por fecha descendente por defecto si no está definido en pageable
        if (pageable.getSort().isUnsorted()) {
            query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fecha"));
        }

        // 5. Aplicar paginación
        query.with(pageable);

        List<Auditoria> logs = mongoTemplate.find(query, Auditoria.class);
        return new PageImpl<>(logs, pageable, total);
    }
}

