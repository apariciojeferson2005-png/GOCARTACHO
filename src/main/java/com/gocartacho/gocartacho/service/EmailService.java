package com.gocartacho.gocartacho.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:8081}")
    private String frontendUrl;

    public void enviarCorreoRecuperacion(String destino, String token) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destino);
        mensaje.setSubject("Recuperación de Contraseña - GO CARTACHO");

        // Reemplaza localhost por tu dominio real en producción
        String urlRecuperacion = frontendUrl + "/reset-password?token=" + token;

        mensaje.setText("Hola,\n\nHas solicitado restablecer tu contraseña en GO CARTACHO.\n"
                + "Haz clic en el siguiente enlace para crear una nueva contraseña:\n\n"
                + urlRecuperacion + "\n\n"
                + "Este enlace expirará por seguridad en 15 minutos.\nSi no solicitaste este cambio, ignora este correo.");
        mailSender.send(mensaje);
    }

    public void enviarCorreoEliminacionCuenta(String destino, String nombreUsuario, String motivo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destino);
            mensaje.setSubject("Tu cuenta ha sido eliminada por moderación - GO CARTACHO");
            mensaje.setText("Hola " + (nombreUsuario != null ? nombreUsuario : "usuario") + ",\n\n"
                    + "Te informamos que tu cuenta en GO CARTACHO ha sido dada de baja por un administrador"
                    + (motivo != null && !motivo.isBlank() ? " debido al siguiente motivo:\n\n" + motivo : " por infringir las normas de conducta de la plataforma.")
                    + "\n\nSi crees que esto ha sido un error, por favor ponte en contacto con soporte.\n\nAtentamente,\nEl equipo de GO CARTACHO");
            mailSender.send(mensaje);
        } catch (Exception e) {
            // Ignorar fallas de envío para no impedir el borrado
        }
    }
}