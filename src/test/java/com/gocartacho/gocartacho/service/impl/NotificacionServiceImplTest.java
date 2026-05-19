package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.Notificacion;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
public class NotificacionServiceImplTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @InjectMocks
    private NotificacionServiceImpl notificacionService;

    @Test
    void enviarNotificacion_Exito() {
        Usuario usuario = new Usuario();
        usuario.setUsuarioId("user-456");

        notificacionService.enviarNotificacion(usuario, "Test Titulo", "Test Mensaje");

        verify(notificacionRepository, times(1)).save(any(Notificacion.class));
    }

    @Test
    void enviarNotificacion_UsuarioNulo_NoHaceNada() {
        notificacionService.enviarNotificacion(null, "Titulo", "Mensaje");
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void marcarComoLeida_Exito() {
        Notificacion notif = new Notificacion();
        notif.setId("notif-1");
        notif.setLeida(false);

        when(notificacionRepository.findById("notif-1")).thenReturn(Optional.of(notif));

        notificacionService.marcarComoLeida("notif-1");

        assertTrue(notif.isLeida());
        verify(notificacionRepository, times(1)).save(notif);
    }
}
