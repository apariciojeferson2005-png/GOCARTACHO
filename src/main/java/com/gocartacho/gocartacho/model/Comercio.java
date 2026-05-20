package com.gocartacho.gocartacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import jakarta.validation.constraints.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;

/**
 * Representa un negocio o lugar de interés dentro de la plataforma.
 * Almacena información detallada, ubicación geoespacial y su estado de aprobación.
 */
@Document(collection = "comercios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comercio implements Serializable {

    @Id
    private String comercioId;

    @NotBlank(message = "El nombre del comercio es obligatorio")
    @Size(max = 255)
    @Field("nombre")
    private String nombre;

    @Field("descripcion")
    private String descripcion;

    @Size(max = 255)
    @Field("direccion")
    private String direccion;

    @NotNull(message = "La latitud es obligatoria para ubicar el negocio en el mapa")
    @DecimalMin(value = "-90.0", message = "Latitud fuera de rango")
    @DecimalMax(value = "90.0", message = "Latitud fuera de rango")
    @Field("latitud")
    private BigDecimal latitud;

    @NotNull(message = "La longitud es obligatoria para ubicar el negocio en el mapa")
    @DecimalMin(value = "-180.0", message = "Longitud fuera de rango")
    @DecimalMax(value = "180.0", message = "Longitud fuera de rango")
    @Field("longitud")
    private BigDecimal longitud;

    @NotNull(message = "Debe especificar un tipo de negocio")
    @Field("tipo_negocio_id")
    private Long tipoNegocioId;

    @Transient
    private String tipoNegocioNombre;

    @Field("horario_apertura")
    private LocalTime horarioApertura;

    @Field("horario_cierre")
    private LocalTime horarioCierre;

    @Size(max = 20)
    @Pattern(regexp = "^\\+?\\d*$", message = "Formato de teléfono inválido")
    @Field("telefono")
    private String telefono;

    @Email(message = "Debe proporcionar un email de contacto válido")
    @Size(max = 100)
    @Field("email_contacto")
    private String emailContacto;

    @Size(max = 255)
    @Field("sitio_web")
    private String sitioWeb;

    @Size(max = 500)
    @Field("imagen_url")
    private String imagenUrl;

    @Builder.Default
    @Field("estado_aprobacion")
    private EstadoComercio estadoAprobacion = EstadoComercio.PENDIENTE;

    @Builder.Default
    @Field("promedio_calificacion")
    private Double promedioCalificacion = 0.0;

    @Builder.Default
    @Field("total_resenas")
    private Integer totalResenas = 0;

    // --- RELACIONES (MongoDB References) ---

    @Field(name = "zona_id", targetType = org.springframework.data.mongodb.core.mapping.FieldType.OBJECT_ID)
    private String zonaId;

    @Field(name = "propietario_id", targetType = org.springframework.data.mongodb.core.mapping.FieldType.OBJECT_ID)
    private String propietarioId;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint ubicacion;

    @Transient
    private String zonaNombre; // Para enriquecer la respuesta si es necesario sin guardarlo en DB
}