package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.Notificacion;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.repository.NotificacionRepository;
import com.gocartacho.gocartacho.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Implementación de la lógica de negocio para Notificacion.
 */
@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;

    @Override
    @Transactional
    public void enviarNotificacion(Usuario usuario, String titulo, String mensaje) {
        if (usuario == null || usuario.getUsuarioId() == null) return;
        Notificacion n = new Notificacion();
        n.setUsuarioId(usuario.getUsuarioId());
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setFecha(LocalDateTime.now());
        notificacionRepository.save(n);
    }

    @Override
    public List<Notificacion> obtenerPorUsuario(String usuarioId) {
        if (usuarioId == null) return Collections.emptyList();
        return notificacionRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);
    }

    @Override
    @Transactional
    public void marcarComoLeida(String notificacionId) {
        if (notificacionId == null) return;
        notificacionRepository.findById(notificacionId).ifPresent(n -> {
            n.setLeida(true);
            notificacionRepository.save(n);
        });
    }

    @Override
    @Transactional
    public void eliminarNotificacion(String notificacionId) {
        if (notificacionId == null) return;
        notificacionRepository.deleteById(notificacionId);
    }
}

