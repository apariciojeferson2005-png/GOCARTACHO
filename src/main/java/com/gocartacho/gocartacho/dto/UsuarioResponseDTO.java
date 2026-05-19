package com.gocartacho.gocartacho.dto;

import com.gocartacho.gocartacho.model.Usuario;

/**
 * Objeto de Transferencia de Datos (DTO) para UsuarioResponseDTO.
 * Utilizado para encapsular los datos enviados y recibidos a traves de la API REST.
 */
public class UsuarioResponseDTO {
    private String usuarioId;
    private String email;
    private String nombre;
    private String apellido;
    private String username;
    private String rol;
    private String fotoUrl;
    private String biografia;
    private String tipoViajero;
    private String proveedor;
    private String estado;

    public UsuarioResponseDTO() {
    }

    public UsuarioResponseDTO(Usuario usuario) {
        this.usuarioId = usuario.getUsuarioId();
        this.email = usuario.getEmail();
        this.nombre = usuario.getNombre();
        this.apellido = usuario.getApellido();
        this.username = usuario.getUsername();
        this.rol = usuario.getRol() != null ? usuario.getRol().name() : null;
        this.fotoUrl = usuario.getFotoUrl();
        this.biografia = usuario.getBiografia();
        this.tipoViajero = usuario.getTipoViajero();
        this.proveedor = usuario.getProveedor() != null ? usuario.getProveedor().name() : null;
        this.estado = usuario.getEstado() != null ? usuario.getEstado().name() : "ACTIVO";
    }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public String getBiografia() { return biografia; }
    public void setBiografia(String biografia) { this.biografia = biografia; }
    public String getTipoViajero() { return tipoViajero; }
    public void setTipoViajero(String tipoViajero) { this.tipoViajero = tipoViajero; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}

