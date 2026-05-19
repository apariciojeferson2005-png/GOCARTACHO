package com.gocartacho.gocartacho.dto;

import java.math.BigDecimal;

/**
 * Objeto de Transferencia de Datos (DTO) para ComercioPlanDTO.
 * Utilizado para encapsular los datos enviados y recibidos a traves de la API REST.
 */
public class ComercioPlanDTO {
    private String comercioId;
    private String nombre;
    private Long tipoNegocioId;
    private String tipoNegocio;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private Double distanciaDesdeAnteriorKm;

    public ComercioPlanDTO() {}

    public ComercioPlanDTO(String comercioId, String nombre, Long tipoNegocioId, String tipoNegocio, BigDecimal latitud, BigDecimal longitud) {
        this.comercioId = comercioId;
        this.nombre = nombre;
        this.tipoNegocioId = tipoNegocioId;
        this.tipoNegocio = tipoNegocio;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getComercioId() {
        return comercioId;
    }

    public void setComercioId(String comercioId) {
        this.comercioId = comercioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getTipoNegocioId() {
        return tipoNegocioId;
    }

    public void setTipoNegocioId(Long tipoNegocioId) {
        this.tipoNegocioId = tipoNegocioId;
    }

    public String getTipoNegocio() {
        return tipoNegocio;
    }

    public void setTipoNegocio(String tipoNegocio) {
        this.tipoNegocio = tipoNegocio;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    public Double getDistanciaDesdeAnteriorKm() {
        return distanciaDesdeAnteriorKm;
    }

    public void setDistanciaDesdeAnteriorKm(Double distanciaDesdeAnteriorKm) {
        // Redondear a 2 decimales
        this.distanciaDesdeAnteriorKm = distanciaDesdeAnteriorKm != null ? Math.round(distanciaDesdeAnteriorKm * 100.0) / 100.0 : null;
    }
}

