package com.gocartacho.gocartacho.dto;

import java.math.BigDecimal;
import com.gocartacho.gocartacho.model.EstadoComercio;

/**
 * Objeto de Transferencia de Datos (DTO) para ComercioDTO.
 * Utilizado para encapsular los datos enviados y recibidos a traves de la API REST.
 */
public class ComercioDTO {
    private String id;
    private String nombre;
    private String descripcion;
    private String direccion;
    private Long tipoNegocioId;
    private String tipoNegocio;
    private String horarioApertura;
    private String horarioCierre;
    private String telefono;
    private String emailContacto;
    private String sitioWeb;
    private String imagenUrl;
    private String zonaId;
    private String zonaNombre;
    private String propietarioId;
    private EstadoComercio estadoAprobacion;
    private int usuariosActuales;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private Boolean horarioAbierto;
    private Double promedioCalificacion;
    private Integer totalResenas;
    private String recomendacion;

    public ComercioDTO() {
    }

    public ComercioDTO(String id, String nombre, Long tipoNegocioId, String tipoNegocio, int usuariosActuales) {
        this.id = id;
        this.nombre = nombre;
        this.tipoNegocioId = tipoNegocioId;
        this.tipoNegocio = tipoNegocio;
        this.usuariosActuales = usuariosActuales;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public Long getTipoNegocioId() { return tipoNegocioId; }
    public void setTipoNegocioId(Long tipoNegocioId) { this.tipoNegocioId = tipoNegocioId; }
    public String getTipoNegocio() { return tipoNegocio; }
    public void setTipoNegocio(String tipoNegocio) { this.tipoNegocio = tipoNegocio; }
    public String getHorarioApertura() { return horarioApertura; }
    public void setHorarioApertura(String horarioApertura) { this.horarioApertura = horarioApertura; }
    public String getHorarioCierre() { return horarioCierre; }
    public void setHorarioCierre(String horarioCierre) { this.horarioCierre = horarioCierre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmailContacto() { return emailContacto; }
    public void setEmailContacto(String emailContacto) { this.emailContacto = emailContacto; }
    public String getSitioWeb() { return sitioWeb; }
    public void setSitioWeb(String sitioWeb) { this.sitioWeb = sitioWeb; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public String getZonaId() { return zonaId; }
    public void setZonaId(String zonaId) { this.zonaId = zonaId; }
    public String getZonaNombre() { return zonaNombre; }
    public void setZonaNombre(String zonaNombre) { this.zonaNombre = zonaNombre; }
    public String getPropietarioId() { return propietarioId; }
    public void setPropietarioId(String propietarioId) { this.propietarioId = propietarioId; }
    public EstadoComercio getEstadoAprobacion() { return estadoAprobacion; }
    public void setEstadoAprobacion(EstadoComercio estadoAprobacion) { this.estadoAprobacion = estadoAprobacion; }
    public int getUsuariosActuales() { return usuariosActuales; }
    public void setUsuariosActuales(int usuariosActuales) { this.usuariosActuales = usuariosActuales; }
    public BigDecimal getLatitud() { return latitud; }
    public void setLatitud(BigDecimal latitud) { this.latitud = latitud; }
    public BigDecimal getLongitud() { return longitud; }
    public void setLongitud(BigDecimal longitud) { this.longitud = longitud; }
    public Boolean getHorarioAbierto() { return horarioAbierto; }
    public void setHorarioAbierto(Boolean horarioAbierto) { this.horarioAbierto = horarioAbierto; }
    public Double getPromedioCalificacion() { return promedioCalificacion; }
    public void setPromedioCalificacion(Double promedioCalificacion) { this.promedioCalificacion = promedioCalificacion; }
    public Integer getTotalResenas() { return totalResenas; }
    public void setTotalResenas(Integer totalResenas) { this.totalResenas = totalResenas; }
    public String getRecomendacion() { return recomendacion; }
    public void setRecomendacion(String recomendacion) { this.recomendacion = recomendacion; }
}

