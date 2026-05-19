package com.gocartacho.gocartacho.dto;

/**
 * Objeto de Transferencia de Datos (DTO) para PuntoMapaCalorDTO.
 * Utilizado para encapsular los datos enviados y recibidos a traves de la API REST.
 */
public class PuntoMapaCalorDTO {
    private double lat;
    private double lng;
    private double intensity;

    public PuntoMapaCalorDTO(double lat, double lng, double intensity) {
        this.lat = lat;
        this.lng = lng;
        this.intensity = intensity;
    }

    // Getters y Setters
    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getIntensity() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }
}
