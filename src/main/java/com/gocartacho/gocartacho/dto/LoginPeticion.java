package com.gocartacho.gocartacho.dto;

/**
 * Objeto de Transferencia de Datos (DTO) para LoginPeticion.
 * Utilizado para encapsular los datos enviados y recibidos a traves de la API REST.
 */
public class LoginPeticion {
    private String identificador;
    private String contrasena;

    // Getters y Setters
    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}

