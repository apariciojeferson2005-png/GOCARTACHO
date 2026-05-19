package com.gocartacho.gocartacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "auditoria")
public class Auditoria {

    @Id
    private String id;

    @Field("admin_email")
    private String adminEmail;

    @Field("accion")
    private String accion; // Ej: "APROBAR_NEGOCIO", "BORRAR_RESENA", "CREAR_ADMIN"

    @Field("entidad")
    private String entidad; // Ej: "COMERCIO", "RESENA", "USUARIO"

    @Field("entidad_id")
    private String entidadId;

    @Field("detalles")
    private String detalles;

    @Field("fecha")
    private LocalDateTime fecha;
}
