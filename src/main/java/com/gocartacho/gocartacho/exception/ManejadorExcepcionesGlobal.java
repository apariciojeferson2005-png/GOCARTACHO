package com.gocartacho.gocartacho.exception;

import com.gocartacho.gocartacho.dto.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ManejadorExcepcionesGlobal {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManejadorExcepcionesGlobal.class);

    /**
     * Manejo de Acceso Denegado (403)
     * Captura las excepciones lanzadas por @PreAuthorize en los controladores.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request,
            HttpServletResponse response) {
        LOGGER.warn("Acceso denegado a {}: {}", request.getRequestURI(), ex.getMessage());

        // Limpiar cookie de sesión si los permisos están comprometidos
        limpiarCookieSesion(request, response);

        if (isApiRequest(request)) {
            return construirRespuestaJson(
                    HttpStatus.FORBIDDEN,
                    "No tienes permisos suficientes para realizar esta acción",
                    null
            );
        }
        return "redirect:/login?error=denied";
    }

    /**
     * Manejo de No Autenticado / Credenciales Inválidas (401)
     * Captura errores de autenticación a nivel de controlador.
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public Object handleAuthenticationException(Exception ex, HttpServletRequest request,
            HttpServletResponse response) {
        LOGGER.warn("Intento de acceso no autenticado o fallo de credenciales a {}: {}", request.getRequestURI(), ex.getMessage());

        limpiarCookieSesion(request, response);

        if (isApiRequest(request)) {
            return construirRespuestaJson(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciales incorrectas o tu sesión ha expirado",
                    null
            );
        }
        return "redirect:/login";
    }

    /**
     * Manejo de Validaciones (400)
     * Captura los errores de validación de los DTOs usando @Valid.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        LOGGER.warn("Errores de validación en DTO detectados: {}", fieldErrors);

        return construirRespuestaJson(
                HttpStatus.BAD_REQUEST,
                "Falla en la validación de los datos de entrada",
                fieldErrors
        );
    }

    /**
     * Manejo de Errores Lógicos de Negocio (400)
     * Utilizado para validaciones de IDs no encontrados y reglas de negocio.
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public Object handleIllegalArgumentException(RuntimeException ex, HttpServletRequest request) {
        LOGGER.warn("Petición inválida o excepción de negocio en {}: {}", request.getRequestURI(), ex.getMessage());

        if (isApiRequest(request)) {
            return construirRespuestaJson(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage(),
                    null
            );
        }

        // Si es navegación web, redirigimos con un parámetro de error para que la vista lo maneje
        return "redirect:/?error=bad_request";
    }

    /**
     * Manejo de No Encontrado (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleNoHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        LOGGER.warn("Recurso no encontrado: {}", request.getRequestURI());
        if (isApiRequest(request)) {
            return construirRespuestaJson(
                    HttpStatus.NOT_FOUND,
                    "El recurso solicitado no existe.",
                    null
            );
        }
        return "redirect:/?error=not_found";
    }

    /**
     * Manejo Genérico de Errores (500)
     * Failsafe para que la aplicación no exponga trazas del servidor al cliente.
     */
    @ExceptionHandler(Exception.class)
    public Object handleGlobalException(Exception ex, HttpServletRequest request) {
        LOGGER.error("Error no controlado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        if (isApiRequest(request)) {
            return construirRespuestaJson(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ha ocurrido un error inesperado en el servidor. Por favor contacta al soporte técnico.",
                    null
            );
        }
        return "redirect:/?error=server";
    }

    // --- Utilidades ---
    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }

    private ResponseEntity<ErrorResponseDTO> construirRespuestaJson(
            HttpStatus status, String message, Map<String, String> detalles) {
        
        ErrorResponseDTO response = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .mensaje(message)
                .detalles(detalles)
                .build();
        return ResponseEntity.status(status).body(response);
    }

    private void limpiarCookieSesion(HttpServletRequest request, HttpServletResponse response) {
        org.springframework.http.ResponseCookie jsessionCookie = org.springframework.http.ResponseCookie
                .from("JSESSIONID", "")
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, jsessionCookie.toString());

        org.springframework.http.ResponseCookie jwtCookie = org.springframework.http.ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, jwtCookie.toString());
    }
}