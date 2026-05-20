package com.gocartacho.gocartacho.security;

import com.gocartacho.gocartacho.model.ProveedorAuth;
import com.gocartacho.gocartacho.model.RolUsuario;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UtilidadJwt jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 Login exitoso. Procesando datos de usuario...");
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String sub = oAuth2User.getAttribute("sub");

        log.info("Datos recibidos de Google: email={}, name={}, sub={}", email, name, sub);

        if (email == null) {
            log.error("Error: El email no fue proporcionado por Google.");
            throw new ServletException("Email no encontrado en Google");
        }

        // Buscar o crear en Mongo
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        Usuario usuario;

        try {
            if (usuarioOpt.isPresent()) {
                usuario = usuarioOpt.get();
                log.info("Usuario existente encontrado: {}. Actualizando información...", email);
                usuario.setNombre(name);
                usuario.setFotoUrl(picture);
                if (usuario.getProveedorId() == null)
                    usuario.setProveedorId(sub);
                usuario = usuarioRepository.save(usuario);
            } else {
                log.info("Creando nuevo usuario para: {}", email);
                usuario = new Usuario();
                usuario.setEmail(email);
                usuario.setNombre(name);
                usuario.setFotoUrl(picture);
                usuario.setProveedor(ProveedorAuth.GOOGLE);
                usuario.setProveedorId(sub);
                usuario.setRol(RolUsuario.USER);

                // IMPORTANTE: Evitar conflictos con el índice único de username
                String baseUsername = email.split("@")[0];
                String finalUsername = baseUsername;
                int counter = 1;
                while (usuarioRepository.existsByUsername(finalUsername)) {
                    finalUsername = baseUsername + counter++;
                }
                usuario.setUsername(finalUsername);

                // Contraseña vacía para usuarios OAuth2 (UserDetails la requiere no nula)
                usuario.setContrasena("");

                usuario = usuarioRepository.save(usuario);
                log.info("Nuevo usuario creado con username: {}", finalUsername);
            }
        } catch (Exception e) {
            log.error("Error al guardar el usuario en MongoDB: {}", e.getMessage(), e);
            throw new ServletException("Error al procesar el usuario en la base de datos", e);
        }

        // Generar JWT
        String token = jwtUtil.generateToken(usuario.getEmail());
        log.info("JWT generado para el usuario: {}", usuario.getEmail());

        // Guardar cookie JWT
        org.springframework.http.ResponseCookie jwtCookie = org.springframework.http.ResponseCookie
                .from("jwt", java.util.Objects.requireNonNull(token))
                .httpOnly(true)
                .secure(request.isSecure()) // Configuración dinámica para soportar HTTP local y HTTPS en producción
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict") // Actualizado a Strict para bloquear ataques CSRF modernos
                .build();

        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, jwtCookie.toString());
        log.info("Cookie 'jwt' añadida a la respuesta.");

        // Redirige a /mapa con el token en la URL para que el JS lo guarde en
        // localStorage
        log.info("Redirigiendo a /mapa con token");
        getRedirectStrategy().sendRedirect(request, response, "/mapa?token=" + token);
    }
}
