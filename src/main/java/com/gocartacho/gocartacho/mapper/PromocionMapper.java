package com.gocartacho.gocartacho.mapper;

import com.gocartacho.gocartacho.dto.PromocionDTO;
import com.gocartacho.gocartacho.model.Promocion;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilidad para mapear entre la entidad Promocion y PromocionDTO.
 * Inyecta el ComercioService para resolver el nombre y coordenadas del comercio asociado.
 */
@Component
public class PromocionMapper {

    private final com.gocartacho.gocartacho.service.ComercioService comercioService;
    private final com.gocartacho.gocartacho.service.ZonaService zonaService;

    public PromocionMapper(com.gocartacho.gocartacho.service.ComercioService comercioService,
                           com.gocartacho.gocartacho.service.ZonaService zonaService) {
        this.comercioService = comercioService;
        this.zonaService = zonaService;
    }

    public PromocionDTO toDto(Promocion promocion) {
        if (promocion == null) {
            return null;
        }
        PromocionDTO dto = new PromocionDTO();
        dto.setId(promocion.getPromocionId());
        dto.setTitulo(promocion.getTitulo());
        dto.setDescripcion(promocion.getDescripcion());
        dto.setPorcentajeDescuento(promocion.getPorcentajeDescuento());
        dto.setFechaInicio(promocion.getFechaInicio());
        dto.setFechaFin(promocion.getFechaFin());
        dto.setActiva(Boolean.TRUE.equals(promocion.getActiva()));
        
        if (promocion.getComercioId() != null) {
            com.gocartacho.gocartacho.model.Comercio comercio = comercioService.obtenerComercioPorId(promocion.getComercioId());
            if (comercio != null) {
                dto.setComercioNombre(comercio.getNombre());
                dto.setComercioId(comercio.getComercioId());
                dto.setZonaId(comercio.getZonaId());
                dto.setLatitud(comercio.getLatitud() != null ? comercio.getLatitud().doubleValue() : null);
                dto.setLongitud(comercio.getLongitud() != null ? comercio.getLongitud().doubleValue() : null);
                
                if (comercio.getZonaId() != null) {
                    try {
                        com.gocartacho.gocartacho.model.Zona zona = zonaService.obtenerZonaPorId(comercio.getZonaId());
                        if (zona != null) {
                            dto.setZonaNumero(zona.getNumero());
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
        
        return dto;
    }

    public List<PromocionDTO> toDtoList(List<Promocion> promociones) {
        if (promociones == null) {
            return Collections.emptyList();
        }
        return promociones.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Promocion toEntity(PromocionDTO dto) {
        if (dto == null) {
            return null;
        }
        Promocion promocion = new Promocion();
        promocion.setTitulo(dto.getTitulo());
        promocion.setDescripcion(dto.getDescripcion());
        promocion.setPorcentajeDescuento(dto.getPorcentajeDescuento());
        promocion.setFechaInicio(dto.getFechaInicio());
        promocion.setFechaFin(dto.getFechaFin());
        promocion.setActiva(dto.isActiva());
        return promocion;
    }
}
