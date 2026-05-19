package com.gocartacho.gocartacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.*;

import java.io.Serializable;

/**
 * Documento MongoDB que representa un usuario de la plataforma.
 * Incluye datos de autenticación y su rol para el control de acceso.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "usuarios")
public class Usuario implements Serializable {

    @Id
    private String usuarioId;

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @Field("contrasena")
    private String contrasena;

    @Field("nombre")
    private String nombre;

    @Field("apellido")
    private String apellido;

    @Indexed(unique = true)
    @Field("username")
    private String username;

    @Builder.Default
    @Field("proveedor")
    private ProveedorAuth proveedor = ProveedorAuth.LOCAL;

    @Field("proveedor_id")
    private String proveedorId;

    @Field("foto_url")
    private String fotoUrl;

    @Field("biografia")
    private String biografia;

    @Field("tipo_viajero")
    private String tipoViajero;

    /**
     * Rol del usuario para autorización.
     * Solo acepta valores USER o ADMIN.
     */
    @Field("rol")
    private RolUsuario rol;

    @Builder.Default
    @Field("estado")
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;
}