package com.gocartacho.gocartacho.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Objeto de Transferencia de Datos (DTO) para ResenaRequest.
 * Utilizado para encapsular los datos enviados y recibidos a traves de la API REST.
 */
public class ResenaRequest {

    @NotNull(message = "La calificación es obligatoria")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private Integer calificacion;

    @Size(max = 1000, message = "El comentario no puede exceder los 1000 caracteres")
    private String comentario;

    private String usuarioId;

    @NotNull(message = "El ID del comercio es obligatorio")
    private String comercioId;

    public Integer getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Integer calificacion) {
        this.calificacion = calificacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getComercioId() {
        return comercioId;
    }

    public void setComercioId(String comercioId) {
        this.comercioId = comercioId;
    }
}

