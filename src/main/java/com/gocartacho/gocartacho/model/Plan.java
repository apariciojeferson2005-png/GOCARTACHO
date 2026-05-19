package com.gocartacho.gocartacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * Representa un itinerario o recorrido temático predefinido (ej: Plan Colonial).
 * Los comercios que pertenecen a esta plan se asocian a través de la colección PlanComercio.
 */
@Document(collection = "planes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan implements Serializable {

    @Id
    private String planId;

    @Field("nombre_plan")
    private String nombrePlan;

    @Field("descripcion")
    private String descripcion;

    @Field("promedio_calificacion")
    private Double promedioCalificacion = 0.0;
}