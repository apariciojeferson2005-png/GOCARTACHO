package com.gocartacho.gocartacho.dto;

/**
 * Objeto de Transferencia de Datos (DTO) para UsuarioUpdateDTO.
 * Utilizado para encapsular los datos enviados y recibidos a traves de la API REST.
 */
public class UsuarioUpdateDTO {
    private String nombre;
    private String apellido;
    private String username;
    private String email;
    private String contrasena;
    private String rol;

    private String estado;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
// Fin de DTO. Archivo recargado para limpiar caché del IDE.


