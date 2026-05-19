package com.gocartacho.gocartacho.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Objeto de Transferencia de Datos (DTO) para PromocionDTO.
 * Utilizado para encapsular los datos enviados y recibidos a traves de la API REST.
 */
public class PromocionDTO {
    private String id;
    private String titulo;
    private String descripcion;
    private BigDecimal porcentajeDescuento;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private boolean activa;
    private String comercioNombre;
    private String comercioId;
    private String zonaId;
    private Integer zonaNumero;
    private Double latitud;
    private Double longitud;

    public PromocionDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPorcentajeDescuento() { return porcentajeDescuento; }
    public void setPorcentajeDescuento(BigDecimal porcentajeDescuento) { this.porcentajeDescuento = porcentajeDescuento; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public String getComercioNombre() { return comercioNombre; }
    public void setComercioNombre(String comercioNombre) { this.comercioNombre = comercioNombre; }

    public String getComercioId() { return comercioId; }
    public void setComercioId(String comercioId) { this.comercioId = comercioId; }

    public String getZonaId() { return zonaId; }
    public void setZonaId(String zonaId) { this.zonaId = zonaId; }

    public Integer getZonaNumero() { return zonaNumero; }
    public void setZonaNumero(Integer zonaNumero) { this.zonaNumero = zonaNumero; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
}
