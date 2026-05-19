package com.gocartacho.gocartacho.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serializable;

/**
 * Entidad de MySQL que representa los tipos de negocio disponibles.
 */
@Entity
@Table(name = "tipos_negocio")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoNegocio implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del tipo de negocio es obligatorio")
    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(length = 500)
    private String descripcion;
}