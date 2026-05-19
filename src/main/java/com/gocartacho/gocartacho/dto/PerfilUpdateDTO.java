package com.gocartacho.gocartacho.dto;

import jakarta.validation.constraints.Size;

/**
 * Objeto de Transferencia de Datos (DTO) para PerfilUpdateDTO.
 * Utilizado para encapsular los datos enviados y recibidos a traves de la API REST.
 */
public class PerfilUpdateDTO {
    
    private String nombre;
    private String apellido;
    private String username;
    private String fotoUrl;
    private String biografia;
    private String tipoViajero;
    
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String contrasena;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public String getBiografia() { return biografia; }
    public void setBiografia(String biografia) { this.biografia = biografia; }
    public String getTipoViajero() { return tipoViajero; }
    public void setTipoViajero(String tipoViajero) { this.tipoViajero = tipoViajero; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}


