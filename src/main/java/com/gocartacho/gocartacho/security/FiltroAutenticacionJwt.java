package com.gocartacho.gocartacho.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de seguridad que intercepta cada petición HTTP (OncePerRequestFilter).
 * Extrae el token JWT (del header Authorization o de la cookie 'jwt'),
 * lo valida y establece el contexto de seguridad si el token es correcto.
 */
@Component
@RequiredArgsConstructor
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private final UtilidadJwt jwtUtil;
    private final DetalleUsuarioServicio customUserDetailsService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FiltroAutenticacionJwt.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String jwt = null;
        final String userEmail;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            // Protección CSRF: Solo permitir lectura de la cookie para peticiones seguras
            // (GET, HEAD, OPTIONS)
            // Para POST/PUT/DELETE/PATCH el frontend debe obligatoriamente enviar el header
            // Authorization
            String method = request.getMethod();
            if (method.equals("GET") || method.equals("HEAD") || method.equals("OPTIONS")) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        break;
                    }
                }
            } else {
                log.warn(
                        "Prevención CSRF: Se ignoró la cookie JWT en una petición {}. Se requiere header Authorization.",
                        method);
            }
        }

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            userEmail = jwtUtil.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.customUserDetailsService.loadUserByUsername(userEmail);

                if (Boolean.TRUE.equals(jwtUtil.validateToken(jwt, userDetails.getUsername()))) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // Establecemos el contexto con el token validado
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Cualquier alteración al JWT o expiración será atrapada aquí.
            log.warn("Fallo de autenticación JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            // Spring ignorará este usuario y el endpoint arrojará 403 Forbidden si está
            // protegido.
        }
        filterChain.doFilter(request, response);
    }
}
