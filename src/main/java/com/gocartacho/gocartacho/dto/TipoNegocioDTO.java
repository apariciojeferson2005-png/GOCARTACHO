package com.gocartacho.gocartacho.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoNegocioDTO {
    private Long id;
    private String nombre;
    private String descripcion;
}
