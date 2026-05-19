package com.gocartacho.gocartacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;


/**
 * Entidad temporal que representa la ubicación reciente de un dispositivo.
 * Los datos expiran automáticamente mediante un TTL index (2 horas).
 * Es fundamental para la generación dinámica del mapa de calor de la ciudad.
 */
@Document(collection = "puntos_calor")
public class PuntoCalor implements Serializable {

    @Id
    private String puntoId;

    @Field("latitud")
    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud no puede ser menor a -90")
    @DecimalMax(value = "90.0", message = "La latitud no puede ser mayor a 90")
    private BigDecimal latitud;

    @Field("longitud")
    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud no puede ser menor a -180")
    @DecimalMax(value = "180.0", message = "La longitud no puede ser mayor a 180")
    private BigDecimal longitud;

    @Field("timestamp")
    @Indexed(expireAfterSeconds = 7200) // Los puntos se eliminan automáticamente tras 2 horas de inactividad
    private LocalDateTime timestamp;
    
    @Field("dispositivo_hash")
    @Indexed // Optimiza la búsqueda para el proceso de 'Upsert'
    private String dispositivoHash; 

    @org.springframework.data.mongodb.core.index.GeoSpatialIndexed(type = org.springframework.data.mongodb.core.index.GeoSpatialIndexType.GEO_2DSPHERE)
    private org.springframework.data.mongodb.core.geo.GeoJsonPoint ubicacion;

    public String getPuntoId() {
        return puntoId;
    }

    public void setPuntoId(String puntoId) {
        this.puntoId = puntoId;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDispositivoHash() {
        return dispositivoHash;
    }

    public void setDispositivoHash(String dispositivoHash) {
        this.dispositivoHash = dispositivoHash;
    }

    public org.springframework.data.mongodb.core.geo.GeoJsonPoint getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(org.springframework.data.mongodb.core.geo.GeoJsonPoint ubicacion) {
        this.ubicacion = ubicacion;
    }

}