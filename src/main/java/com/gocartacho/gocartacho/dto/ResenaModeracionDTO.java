package com.gocartacho.gocartacho.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Objeto de Transferencia de Datos (DTO) para ResenaModeracionDTO.
 * Utilizado para encapsular los datos enviados y recibidos a traves de la API REST.
 */
@Data
public class ResenaModeracionDTO {
    private String resenaId;
    private String comentario;
    private Integer calificacion;
    private LocalDateTime fecha;
    private String usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private String comercioId;
    private String comercioNombre;
    private long totalReportes;
    private java.util.List<String> detallesReportes;
}


