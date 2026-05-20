package com.gocartacho.gocartacho.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Colección de MongoDB para almacenar reportes específicos de reseñas.
 * Permite que múltiples usuarios reporten una misma reseña por diferentes motivos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "reportes_resenas")
public class ReporteResena implements Serializable {

    @Id
    private String id;

    @Indexed
    @Field("resena_id")
    private String resenaId;

    @Field("usuario_id")
    private String usuarioId; // El usuario que reporta la reseña

    @Field("motivo")
    private String motivo; // Spam, Ofensivo, Falso, etc.

    @Field("detalles")
    private String detalles;

    @Field("fecha")
    private LocalDateTime fecha;
}
