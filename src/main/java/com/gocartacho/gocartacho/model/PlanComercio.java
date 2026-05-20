package com.gocartacho.gocartacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * Entidad de relación que asocia un Comercio a una Plan específica,
 * indicando el orden de la parada en el itinerario.
 */
@Document(collection = "plan_comercios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanComercio implements Serializable {

    @Id
    private String id;

    @Field("plan_id")
    private String planId;

    @Field("comercio_id")
    private String comercioId;

    @Field("orden")
    private Integer orden;

    @Field("recomendacion")
    private String recomendacion;
}