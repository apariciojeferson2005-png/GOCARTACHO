package com.gocartacho.gocartacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Almacena información estadística sobre los niveles de afluencia típicos
 * de una zona, agrupados por día de la semana y hora del día.
 * Utilizado por el sistema de Planes Inteligentes.
 */
@Document(collection = "afluencia_historica")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AfluenciaHistorica {

    @Id
    private String histId;

    @Field("dia_semana")
    private DiaSemana diaSemana;

    @Field("hora")
    private Integer hora;

    @Field("nivel_promedio")
    private NivelAfluencia nivelPromedio;

    @Field(name = "zona_id", targetType = org.springframework.data.mongodb.core.mapping.FieldType.OBJECT_ID)
    private String zonaId;
}