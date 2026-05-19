package com.gocartacho.gocartacho.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de seguridad que implementa Rate Limiting (Bucket4j)
 * específicamente para endpoints críticos de autenticación,
 * previniendo ataques de fuerza bruta y spam de correos.
 */
@Component
public class FiltroRateLimitAuth extends OncePerRequestFilter {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> recuperarBuckets = new ConcurrentHashMap<>();

    private Bucket createLoginBucket() {
        // Límite: 10 intentos por minuto por IP para Login
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createRecuperarBucket() {
        // Límite: 3 intentos por hora por IP para Recuperar Contraseña
        Bandwidth limit = Bandwidth.builder()
                .capacity(3)
                .refillGreedy(3, Duration.ofHours(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();

        if (path.startsWith("/api/v1/auth/login")
                && !loginBuckets.computeIfAbsent(ip, k -> createLoginBucket()).tryConsume(1)) {
            sendErrorResponse(response, "Demasiados intentos de inicio de sesión. Por favor, espere un minuto.");
            return;
        } else if (path.startsWith("/api/v1/auth/recuperar")
                && !recuperarBuckets.computeIfAbsent(ip, k -> createRecuperarBucket()).tryConsume(1)) {
            sendErrorResponse(response, "Demasiadas solicitudes de recuperación. Por favor, espere una hora.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":\"error\", \"message\": \"" + message + "\"}");
    }
}