package com.gocartacho.gocartacho.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Documento MongoDB para reseñas de comercios.
 * Usa Lombok @Data para ser consistente con las entidades JPA (Comercio, Promocion).
 */
@Data
@Document(collection = "resenas")
public class Resena implements Serializable {

    @Id
    private String resenaId;

    @Field("calificacion")
    private Integer calificacion;

    @Field("comentario")
    private String comentario;

    @Field("fecha")
    private LocalDateTime fecha;

    // Relaciones lógicas (NoSQL) con las entidades
    @Field("usuario_id")
    private String usuarioId;

    @Field("comercio_id")
    @Indexed
    private String comercioId;

    @Field("reportada")
    private Boolean reportada = false;

    @Field("imagen_url")
    private String imagenUrl;

    @Field("respuesta_comercio")
    private String respuestaComercio;

    @Field("fecha_respuesta")
    private LocalDateTime fechaRespuesta;
}