package com.gocartacho.gocartacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import jakarta.validation.constraints.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Entidad que define un área o barrio específico dentro de Cartagena.
 * Permite agrupar comercios y calcular estadísticas regionales (ej: nivel de concurrencia).
 */
@Document(collection = "zonas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zona implements Serializable {

    @Id
    private String zonaId;

    @NotBlank(message = "El nombre de la zona es obligatorio")
    @Size(max = 100)
    @Field("nombre")
    private String nombre;

    @Field("descripcion")
    private String descripcion;

    /** Número entero identificador fijo (1=Centro Histórico, 2=Getsemaní, etc.).
     * Permite al frontend referirse a la zona sin conocer el ObjectId de MongoDB. */
    @Field("numero")
    private Integer numero;

    @NotNull(message = "La latitud es requerida")
    @DecimalMin(value = "-90.0", message = "Latitud inválida")
    @DecimalMax(value = "90.0", message = "Latitud inválida")
    @Field("latitud")
    private BigDecimal latitud;

    @NotNull(message = "La longitud es requerida")
    @DecimalMin(value = "-180.0", message = "Longitud inválida")
    @DecimalMax(value = "180.0", message = "Longitud inválida")
    @Field("longitud")
    private BigDecimal longitud;

    @Transient
    @Builder.Default
    private Integer nivelConcurrencia = 5;

}