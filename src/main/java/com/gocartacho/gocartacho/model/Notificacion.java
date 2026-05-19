package com.gocartacho.gocartacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Documento que almacena las notificaciones del sistema para un usuario.
 * Se emplea para informar sobre la aprobación/rechazo de comercios u otras alertas.
 */
@Data
@Document(collection = "notificaciones")
public class Notificacion {

    @Id
    private String id;

    @Field("titulo")
    private String titulo;

    @Field("mensaje")
    private String mensaje;

    @Field("fecha")
    private LocalDateTime fecha;

    private boolean leida = false;

    // Relación con el usuario — guardamos solo el ID para evitar queries extras (N+1)
    @Field("usuario_id")
    @Indexed
    private String usuarioId;
}
