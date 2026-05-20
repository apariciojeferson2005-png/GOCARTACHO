package com.gocartacho.gocartacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidad que registra cuando un usuario marca un comercio como favorito.
 * Utilizada para mostrar preferencias personales en el perfil del usuario.
 */
@Document(collection = "favoritos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorito implements Serializable {

    @Id
    private String id;

    @Field(name = "usuario_id", targetType = org.springframework.data.mongodb.core.mapping.FieldType.OBJECT_ID)
    private String usuarioId;

    @Field(name = "comercio_id", targetType = org.springframework.data.mongodb.core.mapping.FieldType.OBJECT_ID)
    private String comercioId;

    @Field("fecha_agregado")
    @Builder.Default
    private LocalDateTime fechaAgregado = LocalDateTime.now();
}
