package com.gocartacho.gocartacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa un descuento u oferta temporal asociada a un Comercio.
 * Los usuarios pueden ver promociones activas dependiendo de la fecha.
 */
@Document(collection = "promociones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promocion implements Serializable {

    @Id
    private String promocionId;

    @Field("titulo")
    private String titulo;

    @Field("descripcion")
    private String descripcion;

    @Field("porcentaje_descuento")
    private BigDecimal porcentajeDescuento;

    @Field("fecha_inicio")
    private LocalDate fechaInicio;

    @Field("fecha_fin")
    private LocalDate fechaFin;

    @Field("activa")
    private Boolean activa = true;

    // --- Relación con Comercio (Referencia) ---
    @Field(name = "comercio_id", targetType = org.springframework.data.mongodb.core.mapping.FieldType.OBJECT_ID)
    private String comercioId;
}
