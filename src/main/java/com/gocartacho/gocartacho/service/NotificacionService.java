package com.gocartacho.gocartacho.service;

import com.gocartacho.gocartacho.model.Notificacion;
import com.gocartacho.gocartacho.model.Usuario;
import java.util.List;

/**
 * Interfaz que define las operaciones de negocio para Notificacion.
 */
public interface NotificacionService {
    void enviarNotificacion(Usuario usuario, String titulo, String mensaje);
    List<Notificacion> obtenerPorUsuario(String usuarioId);
    void marcarComoLeida(String notificacionId);
    void eliminarNotificacion(String notificacionId);
}

